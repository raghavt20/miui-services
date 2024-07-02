package com.android.server.appcacheopt;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInstalld;
import android.os.IVold;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import com.android.server.MiuiBgThread;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class AppCacheOptimizer implements AppCacheOptimizerStub {
    private static final String CLOUD_CONFIG_FILE_PATH = "/data/system/app_cache_optimization/app_cache_optimization_package_path_cfg.json";
    private static final long CONNECT_RETRY_DELAY_MS = 1000;
    private static final String DEFAULT_CONFIG_FILE_PATH = "/product/etc/app_cache_optimization/app_cache_optimization_package_path_cfg.json";
    private static final long MEM_PRESSURE_REPORT_REFRESH_SECONDS = 30000;
    private static final int REPORT_MEM_PRESSURE_LIMIT = 1;
    private static final String TAG = "APPCacheOptimization";
    private ArrayList<AppCacheOptimizerConfig> mAppConfigs;
    private Context mContext;
    private IInstalld mInstalld;
    private IVold mVold;
    private CloudFileWatcher mWatcher;
    private static boolean DEBUG = SystemProperties.getBoolean("sys.debug.appCacheOptimization", false);
    private static final File CLOUD_CONFIG_FILE_FOLD = new File("/data/system/app_cache_optimization");
    private static final boolean APP_CACHE_OPTIMIZATION_ENABLED = SystemProperties.getBoolean("persist.sys.performance.appCacheOptimization", false);
    private static final boolean APP_CACHE_OPTIMIZATION_MEMORY_CHECK = memoryCheck();
    private final ConcurrentHashMap<String, String[]> mMountDirs = new ConcurrentHashMap<>();
    private final ArrayMap<String, String[]> mParseMountDirs = new ArrayMap<>();
    private final ArrayMap<String, String[]> mParseUnmountDirs = new ArrayMap<>();
    private String mForeground = "";
    private long memPressureStorageUsageWatermark = 200;
    private long mWatermark = FormatBytesUtil.KB;
    private long lastReportMemPressureTime = 0;
    private int reportMemPressureCnt = 0;
    private long totalStorageUsage = 0;
    private boolean mConfigChange = false;
    private boolean mAppCacheOptimizationEnabled = false;
    private boolean mInitialized = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppCacheOptimizer> {

        /* compiled from: AppCacheOptimizer$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppCacheOptimizer INSTANCE = new AppCacheOptimizer();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AppCacheOptimizer m721provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AppCacheOptimizer m720provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.appcacheopt.AppCacheOptimizer is marked as singleton");
        }
    }

    public void onBootCompleted(Context context) {
        if (APP_CACHE_OPTIMIZATION_ENABLED && APP_CACHE_OPTIMIZATION_MEMORY_CHECK) {
            if (context == null) {
                Slog.e(TAG, "onBootCompleted context is null");
                return;
            }
            this.mContext = context;
            updateAppCacheFeatureFromSettings();
            if (!this.mAppCacheOptimizationEnabled) {
                Slog.i(TAG, "onBootCompleted: disable AppCacheOptimization");
            } else {
                Slog.i(TAG, "onBootCompleted: enable AppCacheOptimization");
                connectAndInit();
            }
        }
    }

    public void beforePackageRemoved(int userId, String packageName) {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK) {
            return;
        }
        if (!this.mAppCacheOptimizationEnabled) {
            Slog.i(TAG, "beforePackageRemoved enabled: false");
            return;
        }
        if (userId != 0) {
            return;
        }
        if (this.mContext == null) {
            Slog.e(TAG, "beforePackageRemoved mContext is null");
            return;
        }
        if (isAppRunning(packageName)) {
            Slog.e(TAG, "app is running.");
        }
        Slog.i(TAG, "beforePackageRemoved :" + packageName);
        if (this.mMountDirs.containsKey(packageName)) {
            if (umount(packageName, this.mMountDirs.get(packageName))) {
                Slog.i(TAG, "umount success");
                this.mMountDirs.remove(packageName);
                updatePathMapOnInstalld(packageName, new String[0]);
                return;
            }
            Slog.e(TAG, "umount fail");
        }
    }

    public void onForegroundActivityChanged(String packageName) {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK || !this.mAppCacheOptimizationEnabled) {
            return;
        }
        this.mForeground = packageName;
    }

    public void reportMemPressure(int pressureState) {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK || !this.mAppCacheOptimizationEnabled || reportLowMemPressureLimit(pressureState)) {
            return;
        }
        switch (pressureState) {
            case 2:
                Slog.d(TAG, "MEM_PRESSURE_MEDIUM: release 50% storage of all background APP");
                this.mMountDirs.forEach(new BiConsumer() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda2
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        AppCacheOptimizer.this.lambda$reportMemPressure$0((String) obj, (String[]) obj2);
                    }
                });
                this.totalStorageUsage /= 2;
                return;
            case 3:
                Slog.d(TAG, "MEM_PRESSURE_HIGH: release 80% storage of all background APP, 50% storage of mForeground APP");
                this.mMountDirs.forEach(new BiConsumer() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda3
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        AppCacheOptimizer.this.lambda$reportMemPressure$1((String) obj, (String[]) obj2);
                    }
                });
                String fg = this.mForeground;
                String[] paths = this.mMountDirs.get(fg);
                if (paths != null) {
                    for (String path : paths) {
                        releaseCacheStorageByPercent(fg, path, 50L);
                    }
                }
                this.totalStorageUsage /= 2;
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$reportMemPressure$0(String k, String[] v) {
        if (!k.equals(this.mForeground)) {
            for (String s : v) {
                releaseCacheStorageByPercent(k, s, 50L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$reportMemPressure$1(String k, String[] v) {
        if (!k.equals(this.mForeground)) {
            for (String s : v) {
                releaseCacheStorageByPercent(k, s, 80L);
            }
        }
    }

    private boolean reportLowMemPressureLimit(int pressureState) {
        if (pressureState <= 1 || this.totalStorageUsage <= this.memPressureStorageUsageWatermark) {
            return true;
        }
        if (System.currentTimeMillis() - this.lastReportMemPressureTime >= 30000) {
            this.lastReportMemPressureTime = System.currentTimeMillis();
            this.reportMemPressureCnt = 0;
        }
        int i = this.reportMemPressureCnt;
        if (i >= 1) {
            return true;
        }
        this.reportMemPressureCnt = i + 1;
        return false;
    }

    private void connectAndInit() {
        Slog.d(TAG, "connectAndInit");
        connectInstalld();
        connectVold();
    }

    private void init() {
        if (this.mInstalld == null || this.mVold == null || this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        Slog.d(TAG, "init");
        CloudFileWatcher cloudFileWatcher = new CloudFileWatcher();
        this.mWatcher = cloudFileWatcher;
        cloudFileWatcher.startWatching();
        this.mMountDirs.clear();
        loadMountDirs();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.appcacheopt.AppCacheOptimizer.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    Slog.d(AppCacheOptimizer.TAG, "package add:" + intent.getData().toString());
                    String addPackageName = intent.getData().toString().split(":")[1];
                    AppCacheOptimizer.this.onPackageAdded(addPackageName);
                }
            }
        }, intentFilter);
        loadDefaultConfigData();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean connectInstalld() {
        if (this.mInstalld != null) {
            return true;
        }
        IBinder binder = ServiceManager.getService("installd");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        AppCacheOptimizer.this.lambda$connectInstalld$2();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder == null) {
            Slog.w(TAG, "installd not found; trying again");
            MiuiBgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AppCacheOptimizer.this.connectInstalld();
                }
            }, 1000L);
            return false;
        }
        IInstalld installd = IInstalld.Stub.asInterface(binder);
        this.mInstalld = installd;
        Slog.i(TAG, "installd connect success");
        init();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$connectInstalld$2() {
        Slog.w(TAG, "installd died; reconnecting");
        this.mInstalld = null;
        connectInstalld();
        updateWatermark(this.mWatermark);
        updatePathMapOnInstalld();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean connectVold() {
        if (this.mVold != null) {
            return true;
        }
        IBinder binder = ServiceManager.getService("vold");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda5
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        AppCacheOptimizer.this.lambda$connectVold$3();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder == null) {
            Slog.w(TAG, "vold not found; trying again");
            MiuiBgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    AppCacheOptimizer.this.connectVold();
                }
            }, 1000L);
            return false;
        }
        IVold vold = IVold.Stub.asInterface(binder);
        this.mVold = vold;
        Slog.i(TAG, "vold connect success");
        init();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$connectVold$3() {
        Slog.w(TAG, "vold died; reconnecting");
        this.mVold = null;
        connectVold();
    }

    private void getAbsolutePath(String packageName, String[] path) {
        for (int i = 0; i < path.length; i++) {
            path[i] = "/data/data/" + packageName + "/" + path[i];
        }
    }

    private void loadMountDirs() {
        ArrayMap<String, ArrayList<String>> mMountFileDirs = new ArrayMap<>();
        String data = readLocalFile("/proc/mounts");
        String[] dataSplit = data.split("\n");
        for (int i = 0; i < dataSplit.length; i++) {
            if (dataSplit[i].startsWith("tmpfs /data_mirror") && dataSplit[i].contains("com")) {
                int startIndex = dataSplit[i].indexOf("com");
                int endIndex = dataSplit[i].indexOf(" ", startIndex);
                String absPath = dataSplit[i].substring(startIndex, endIndex);
                String packageName = absPath.substring(0, absPath.indexOf("/"));
                String rePath = absPath.substring(absPath.indexOf("/") + 1);
                if (!mMountFileDirs.containsKey(packageName)) {
                    ArrayList<String> pathArray = new ArrayList<>();
                    pathArray.add(rePath);
                    mMountFileDirs.put(packageName, pathArray);
                } else {
                    mMountFileDirs.get(packageName).add(rePath);
                }
            }
        }
        if (!mMountFileDirs.isEmpty()) {
            for (String key : mMountFileDirs.keySet()) {
                String[] arr = new String[mMountFileDirs.get(key).size()];
                mMountFileDirs.get(key).toArray(arr);
                getAbsolutePath(key, arr);
                Slog.i(TAG, key + " = " + Arrays.toString(arr));
                this.mMountDirs.put(key, arr);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageAdded(String packageName) {
        if (isAppRunning(packageName)) {
            Slog.w(TAG, "app is running.");
        }
        if (this.mParseMountDirs.containsKey(packageName)) {
            Slog.d(TAG, "add Package:" + packageName);
            if (!this.mMountDirs.containsKey(packageName)) {
                Slog.d(TAG, "mount app because package add");
                if (mount(packageName, this.mParseMountDirs.get(packageName))) {
                    Slog.i(TAG, "mount success");
                    this.mMountDirs.put(packageName, this.mParseMountDirs.get(packageName));
                    updatePathMapOnInstalld(packageName, this.mParseMountDirs.get(packageName));
                    return;
                }
                Slog.e(TAG, "mount fail");
                return;
            }
            Slog.e(TAG, "mount skip, because already in mMountDirs");
        }
    }

    public void loadDefaultConfigData() {
        Slog.d(TAG, "loading cloud config file. file path:/data/system/app_cache_optimization/app_cache_optimization_package_path_cfg.json");
        String data = readLocalFile(CLOUD_CONFIG_FILE_PATH);
        if (data.isEmpty()) {
            Slog.d(TAG, "loading local config file. file path:/product/etc/app_cache_optimization/app_cache_optimization_package_path_cfg.json");
            data = readLocalFile(DEFAULT_CONFIG_FILE_PATH);
        }
        parseData(data);
        updateWatermark(this.mWatermark);
        if (this.mAppConfigs != null) {
            Slog.i(TAG, "config file parse success.");
            for (int i = 0; i < this.mAppConfigs.size(); i++) {
                String packageName = this.mAppConfigs.get(i).getPackageName();
                String[] path = this.mAppConfigs.get(i).getOptimizePath();
                HashSet<String> pathSet = new HashSet<>(Arrays.asList(path));
                pathSet.toArray(path);
                if (this.mAppConfigs.get(i).getIsOptimizeEnable() == 1) {
                    if (isAppRunning(packageName)) {
                        Slog.i(TAG, "app is running.");
                    }
                    if (path.length == 1 && path[0].equals("")) {
                        Slog.w(TAG, "path in config file is empty");
                        if (this.mMountDirs.containsKey(packageName)) {
                            if (umount(packageName, this.mMountDirs.get(packageName))) {
                                Slog.i(TAG, "umount success");
                                this.mMountDirs.remove(packageName);
                                updatePathMapOnInstalld(packageName, new String[0]);
                            } else {
                                Slog.e(TAG, "umount fail");
                            }
                        }
                    } else {
                        getAbsolutePath(packageName, path);
                        Slog.i(TAG, packageName + "    " + this.mAppConfigs.get(i).getIsOptimizeEnable() + "    " + Arrays.toString(path));
                        this.mParseMountDirs.put(packageName, path);
                        if (!this.mMountDirs.containsKey(packageName)) {
                            if (mount(packageName, path)) {
                                Slog.i(TAG, "mount success");
                                this.mMountDirs.put(packageName, path);
                                updatePathMapOnInstalld(packageName, path);
                            } else {
                                Slog.e(TAG, "mount fail");
                            }
                        } else {
                            cloudFileChanage(packageName, this.mMountDirs.get(packageName), path);
                        }
                    }
                } else if (this.mAppConfigs.get(i).getIsOptimizeEnable() == 0) {
                    if (isAppRunning(packageName)) {
                        Slog.i(TAG, "app is running.");
                    }
                    getAbsolutePath(packageName, path);
                    this.mParseUnmountDirs.put(packageName, path);
                    if (this.mMountDirs.containsKey(packageName)) {
                        if (umount(packageName, path)) {
                            Slog.i(TAG, "umount success");
                            this.mMountDirs.remove(packageName);
                            updatePathMapOnInstalld(packageName, new String[0]);
                        } else {
                            Slog.e(TAG, "umount fail");
                        }
                    } else {
                        Slog.e(TAG, "umount fail, because not in mMountDirs");
                    }
                }
            }
            return;
        }
        Slog.i(TAG, "配置文件为空");
    }

    private void cloudFileChanage(String packageName, String[] mountPath, String[] path) {
        HashSet<String> mountSet = new HashSet<>(Arrays.asList(mountPath));
        HashSet<String> pathSet = new HashSet<>(Arrays.asList(path));
        HashSet<String> changeMountSet = new HashSet<>(Arrays.asList(mountPath));
        Iterator<String> it = mountSet.iterator();
        while (it.hasNext()) {
            String i = it.next();
            if (pathSet.contains(i)) {
                Slog.w(TAG, "umount skip, because already in mMountDirs");
            } else if (umount(packageName, new String[]{i})) {
                Slog.i(TAG, "umount single dir success");
                changeMountSet.remove(i);
            } else {
                Slog.e(TAG, "umount single dir fail");
            }
        }
        Iterator<String> it2 = pathSet.iterator();
        while (it2.hasNext()) {
            String i2 = it2.next();
            if (mountSet.contains(i2)) {
                Slog.w(TAG, "mount skip, because already in mMountDirs");
            } else if (mount(packageName, new String[]{i2})) {
                Slog.i(TAG, "mount single dir success");
                changeMountSet.add(i2);
            } else {
                Slog.e(TAG, "mount single dir fail");
            }
        }
        String[] changePath = new String[changeMountSet.size()];
        changeMountSet.toArray(changePath);
        this.mMountDirs.put(packageName, changePath);
        updatePathMapOnInstalld(packageName, changePath);
    }

    private void parseData(String data) {
        this.mAppConfigs = AppCacheOptimizerConfigUtils.parseAppConfig(data);
        long tmp = AppCacheOptimizerConfigUtils.parseWatermarkConfig(data);
        this.mWatermark = tmp < 0 ? this.mWatermark : tmp;
    }

    private boolean mount(String packageName, String[] path) {
        Slog.i(TAG, "start mount " + packageName);
        boolean ret = false;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            ret = this.mInstalld.setFileConRecursive(packageName, path, "u:object_r:system_data_file:s0", info.uid, info.uid);
            if (!ret) {
                Slog.e(TAG, "installd setFileConRecursive fail before mount");
            } else if (!this.mVold.mountTmpfs(path, info.uid, info.uid)) {
                Slog.e(TAG, "vold mountTmpfs fail");
                ret = false;
            }
            if (!this.mInstalld.setFileConRecursive(packageName, path, "u:object_r:app_data_file:s0", info.uid, info.uid)) {
                Slog.e(TAG, "installd setFileConRecursive fail after mount");
                return false;
            }
            return ret;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "package:" + packageName + " doesn't exsits!");
            return ret;
        } catch (Exception e2) {
            Slog.e(TAG, "mount exception", e2);
            return ret;
        }
    }

    public boolean umount(String packageName, String[] path) {
        Slog.i(TAG, "start umount " + packageName);
        boolean ret = false;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            ret = this.mInstalld.setFileConRecursive(packageName, path, "u:object_r:system_data_file:s0", info.uid, info.uid);
            if (!ret) {
                Slog.e(TAG, "installd setFileConRecursive fail before umount");
            } else if (!this.mVold.umountTmpfs(path)) {
                Slog.e(TAG, "vold umountTmpfs fail");
                ret = false;
            }
            if (!this.mInstalld.setFileConRecursive(packageName, path, "u:object_r:app_data_file:s0", info.uid, info.uid)) {
                Slog.e(TAG, "installd setFileConRecursive fail after umount");
                return false;
            }
            return ret;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "package:" + packageName + " doesn't exsits!");
            return ret;
        } catch (Exception e2) {
            Slog.e(TAG, "umount exception", e2);
            return ret;
        }
    }

    public static String readLocalFile(String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        File file = new File(filePath);
        if (file.exists()) {
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int lenth = inputStream.read(buffer);
                        if (lenth == -1) {
                            break;
                        }
                        stringBuilder.append(new String(buffer, 0, lenth));
                    }
                    inputStream.close();
                } finally {
                }
            } catch (IOException e) {
                Slog.e(TAG, "exception when readLocalFile: ", e);
            }
        } else {
            Slog.w(TAG, filePath + " does not exist!");
        }
        return stringBuilder.toString();
    }

    private void releaseCacheStorageByPercent(String packageName, String dataPath, long percent) {
        try {
            this.mInstalld.releaseAppStorageByPercent(packageName, dataPath, percent);
        } catch (Exception e) {
            Slog.e(TAG, "releaseAppStorageByPercent failed!", e);
        }
    }

    private void updatePathMapOnInstalld(String packageName, String[] paths) {
        try {
            this.mInstalld.updateAppCacheMountPath(packageName, paths);
        } catch (Exception e) {
            Slog.e(TAG, "updatePathMapOnInstalld failed!", e);
        }
    }

    private void updatePathMapOnInstalld() {
        this.mMountDirs.forEach(new BiConsumer() { // from class: com.android.server.appcacheopt.AppCacheOptimizer$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                AppCacheOptimizer.this.lambda$updatePathMapOnInstalld$4((String) obj, (String[]) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updatePathMapOnInstalld$4(String packageName, String[] paths) {
        try {
            this.mInstalld.updateAppCacheMountPath(packageName, paths);
        } catch (Exception e) {
            Slog.e(TAG, "updatePathMapOnInstalld failed!", e);
        }
    }

    private void updateWatermark(long newWatermark) {
        try {
            this.mInstalld.updateWatermark(newWatermark);
        } catch (Exception e) {
            Slog.e(TAG, "updateWatermark failed!", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CloudFileWatcher extends FileObserver {
        public CloudFileWatcher() {
            super(AppCacheOptimizer.CLOUD_CONFIG_FILE_FOLD, 4095);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            switch (event) {
                case 2:
                    Slog.i(AppCacheOptimizer.TAG, "MODIFY:2 path:" + path);
                    AppCacheOptimizer.this.mParseMountDirs.clear();
                    AppCacheOptimizer.this.mParseUnmountDirs.clear();
                    AppCacheOptimizer.this.loadDefaultConfigData();
                    return;
                case 256:
                    Slog.i(AppCacheOptimizer.TAG, "CREATE:256 path:" + path);
                    return;
                case 512:
                    Slog.i(AppCacheOptimizer.TAG, "DELETE:512 path:" + path);
                    return;
                default:
                    return;
            }
        }
    }

    private boolean isAppRunning(String packageName) {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager.getRunningAppProcesses();
        if (appProcessInfos == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessInfos) {
            if (appProcess.processName.equals(packageName)) {
                Slog.i(TAG, "isAppRunning:" + packageName);
                return true;
            }
        }
        return false;
    }

    public void updateAppStorageUsageInfo() {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK || !this.mAppCacheOptimizationEnabled) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start updateAppStorageUsageInfo");
        }
        try {
            this.totalStorageUsage = this.mInstalld.updateAppStorageUsageInfo();
            if (DEBUG) {
                Slog.d(TAG, "storage usage info updated: " + this.totalStorageUsage + " MB");
            }
        } catch (Exception e) {
            Slog.e(TAG, "updateAppStorageUsageInfo fail!", e);
        }
    }

    public void umountByPackageName(String packageName, int userId) {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK || !this.mAppCacheOptimizationEnabled) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start umountByPackageName");
        }
        try {
            beforePackageRemoved(userId, packageName);
        } catch (Exception e) {
            Slog.e(TAG, "umountByPackageName fail!", e);
        }
    }

    public void mountByPackageName(String packageName) {
        if (!APP_CACHE_OPTIMIZATION_ENABLED || !APP_CACHE_OPTIMIZATION_MEMORY_CHECK || !this.mAppCacheOptimizationEnabled) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start mountByPackageName");
        }
        try {
            onPackageAdded(packageName);
        } catch (Exception e) {
            Slog.e(TAG, "mountByPackageName fail!", e);
        }
    }

    void updateAppCacheFeatureFromSettings() {
        int newValue = Settings.System.getIntForUser(this.mContext.getContentResolver(), "miui_app_cache_optimization", 0, -2);
        if (newValue == 0) {
            if (this.mAppCacheOptimizationEnabled) {
                this.mAppCacheOptimizationEnabled = false;
            }
        } else if (!this.mAppCacheOptimizationEnabled) {
            this.mAppCacheOptimizationEnabled = true;
        }
        notifyAppCacheOptimizationEnabled();
    }

    void notifyAppCacheOptimizationEnabled() {
        if (this.mAppCacheOptimizationEnabled) {
            Slog.d(TAG, "Enable from DeveloperOptions");
        } else {
            Slog.d(TAG, "Disable from DeveloperOptions");
        }
    }

    /* loaded from: classes.dex */
    private class MiuiSettingsObserver extends ContentObserver {
        public MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = AppCacheOptimizer.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("miui_app_cache_optimization"), false, this, -2);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            AppCacheOptimizer.this.updateAppCacheFeatureFromSettings();
        }
    }

    private static boolean memoryCheck() {
        try {
            MemInfoReader memInfo = new MemInfoReader();
            memInfo.readMemInfo();
            long[] infos = memInfo.getRawInfo();
            long temp = (infos[0] / 1000) / 1000;
            int mem_gb = (int) temp;
            if (mem_gb < 23) {
                Slog.d(TAG, "memoryCheck:false, mem: " + mem_gb + " GB");
                return false;
            }
            Slog.d(TAG, "memoryCheck:true, mem: " + mem_gb + " GB");
            return true;
        } catch (ClassCastException cce) {
            Slog.e(TAG, "memoryCheck: ", cce);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "memoryCheck: ", e);
            return false;
        }
    }
}
