package com.android.server.am;

import android.app.ActivityTaskManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioPlaybackConfiguration;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.PssTable;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ProcessPolicy;
import com.android.server.audio.AudioService;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.miui.server.input.edgesuppression.EdgeSuppressionFactory;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.security.AccessControlImpl;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class PeriodicCleanerService extends SystemService {
    private static final int AGGREGATE_HOURS = 24;
    private static final int CACHED_APP_MIN_ADJ = 900;
    private static final String CLOUD_PERIODIC_ENABLE = "cloud_periodic_enable";
    private static final String CLOUD_PERIODIC_GAME_ENABLE = "cloud_periodic_game_enable";
    private static final String CLOUD_PERIODIC_WHITE_LIST = "cloud_periodic_white_list";
    private static boolean DEBUG = false;
    private static final int DEVICE_MEM_TYPE_COUNT = 7;
    private static final int HISTORY_SIZE;
    private static final int KILL_LEVEL_FORCE_STOP = 104;
    private static final int KILL_LEVEL_UNKOWN = 100;
    private static final long MAX_MEMORY_VALUE = 409600;
    private static final int MEM_NO_PRESSURE = -1;
    private static final int MEM_PRESSURE_COUNT = 3;
    private static final int MEM_PRESSURE_CRITICAL = 2;
    private static final int MEM_PRESSURE_LOW = 0;
    private static final int MEM_PRESSURE_MIN = 1;
    private static final int MININUM_AGING_THRESHOLD = 3;
    private static final int MSG_REPORT_CLEAN_PROCESS = 6;
    private static final int MSG_REPORT_EVENT = 1;
    private static final int MSG_REPORT_PRESSURE = 2;
    private static final int MSG_REPORT_START_PROCESS = 5;
    private static final int MSG_SCREEN_OFF = 3;
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PERIODIC_DEBUG_PROP = "persist.sys.periodic.debug";
    private static final String PERIODIC_ENABLE_PROP = "persist.sys.periodic.u.enable";
    private static final String PERIODIC_MEM_THRES_PROP = "persist.sys.periodic.mem_threshold";
    private static final String PERIODIC_START_PROCESS_ENABLE_PROP = "persist.sys.periodic.u.startprocess.enable";
    private static final String PERIODIC_TIME_THRES_PROP = "persist.sys.periodic.time_threshold";
    private static final int SCREEN_STATE_OFF = 2;
    private static final int SCREEN_STATE_ON = 1;
    private static final int SCREEN_STATE_UNKOWN = 3;
    private static final Integer SYSTEM_UID_OBJ;
    private static final String TAG = "PeriodicCleaner";
    private static final String TIME_FORMAT_PATTERN = "HH:mm:ss.SSS";
    private static final long UPDATE_PROCSTATS_PERIOD = 43200000;
    private ActivityManagerService mAMS;
    private ActivityTaskManagerService mATMS;
    private Class<?> mAndroidOsDebugClz;
    private int mAppMemThreshold;
    private AudioService mAudioService;
    private Class<?> mAudioServiceClz;
    private BinderService mBinderService;
    private Class<?> mClassProcessState;
    final CleanInfo[] mCleanHistory;
    private int mCleanHistoryIndex;
    private Context mContext;
    private Field mDebugClz_Field_MEMINFO_BUFFERS;
    private Field mDebugClz_Field_MEMINFO_CACHED;
    private Field mDebugClz_Field_MEMINFO_SHMEM;
    private Field mDebugClz_Field_MEMINFO_SWAPCACHED;
    private Field mDebugClz_Field_MEMINFO_UNEVICTABLE;
    private int mDebugClz_MEMINFO_BUFFERS;
    private int mDebugClz_MEMINFO_CACHED;
    private int mDebugClz_MEMINFO_SHMEM;
    private int mDebugClz_MEMINFO_SWAPCACHED;
    private int mDebugClz_MEMINFO_UNEVICTABLE;
    private volatile boolean mEnable;
    private volatile boolean mEnableFgTrim;
    private volatile boolean mEnableGameClean;
    private volatile boolean mEnableStartProcess;
    private int mFgTrimTheshold;
    private Field mFieldProcessState;
    private int mGamePlayAppNum;
    private Method mGetAllAudioFocusMethod;
    private Method mGetVisibleWindowOwnerMethod;
    private MyHandler mHandler;
    private volatile boolean mHighDevice;
    private Class<?> mIApplicationThreadClz;
    private long mLastCleanByPressure;
    private String mLastNonSystemFgPkg;
    private int mLastNonSystemFgUid;
    private long mLastUpdateTime;
    private List<Integer> mLastVisibleUids;
    private PeriodicCleanerInternalStub mLocalService;
    private final Object mLock;
    private int mLruActiveLength;
    final ArrayList<PackageUseInfo> mLruPackages;
    private int mOverTimeAppNum;
    private PackageManagerService.IPackageManagerImpl mPKMS;
    private ProcessManagerService mPMS;
    private int[] mPressureAgingThreshold;
    private int[] mPressureCacheThreshold;
    private int[] mPressureTimeThreshold;
    private IProcessStats mProcessStats;
    private volatile boolean mReady;
    private BroadcastReceiver mReceiver;
    private Method mScheduleAggressiveTrimMethod;
    private volatile int mScreenState;
    private HandlerThread mThread;
    private UsageStatsManager mUSM;
    private WindowManagerInternal mWMS;
    private Class<?> mWindowManagerInternalClz;
    private static final int MAX_PREVIOUS_TIME = 300000;
    private static int[] sDefaultTimeLevel = {900000, 600000, MAX_PREVIOUS_TIME};
    private static int[] sOverTimeAppNumArray = {2, 3, 3, 4, 6, 6, 6};
    private static int[] sGamePlayAppNumArray = {2, 2, 2, 4, 5, 6, 7};
    private static final int[][] sDefaultActiveLength = {new int[]{3, 2, 2}, new int[]{3, 2, 2}, new int[]{5, 4, 3}, new int[]{7, 6, 5}, new int[]{9, 8, 7}, new int[]{10, 9, 8}, new int[]{14, 13, 12}};
    private static final int[][] sDefaultCacheLevel = {new int[]{450000, 370000, 290000}, new int[]{550000, 470000, 390000}, new int[]{970000, 890000, 810000}, new int[]{1250000, 1170000, 1090000}, new int[]{1650000, 1570000, 1490000}, new int[]{2050000, 1970000, 1890000}, new int[]{2450000, 2370000, 2290000}};
    private static List<String> sSystemRelatedPkgs = new ArrayList();
    private static List<String> sCleanWhiteList = new ArrayList();
    private static List<String> sHomeOrRecents = new ArrayList();
    private static Set<String> sHighFrequencyApp = new ArraySet();
    private static List<String> sHighMemoryApp = new ArrayList();
    private static List<String> sGameApp = new ArrayList();

    static {
        boolean z = SystemProperties.getBoolean(PERIODIC_DEBUG_PROP, false);
        DEBUG = z;
        HISTORY_SIZE = z ? 500 : 100;
        SYSTEM_UID_OBJ = new Integer(1000);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onStart() {
        if (DEBUG) {
            Slog.i(TAG, "Starting PeriodicCleaner");
        }
        this.mBinderService = new BinderService();
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        publishLocalService(PeriodicCleanerInternalStub.class, localService);
        publishBinderService("periodic", this.mBinderService);
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            onActivityManagerReady();
        } else if (phase == 1000) {
            onBootComplete();
        }
    }

    private void onActivityManagerReady() {
        this.mPMS = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        this.mAMS = ServiceManager.getService("activity");
        this.mATMS = ActivityTaskManager.getService();
        this.mWMS = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mPKMS = ServiceManager.getService("package");
        this.mAudioService = ServiceManager.getService("audio");
        this.mProcessStats = IProcessStats.Stub.asInterface(ServiceManager.getService("procstats"));
        UsageStatsManager usageStatsManager = (UsageStatsManager) this.mContext.getSystemService("usagestats");
        this.mUSM = usageStatsManager;
        if (this.mPMS == null || this.mAMS == null || this.mATMS == null || this.mWMS == null || this.mProcessStats == null || this.mPKMS == null || this.mAudioService == null || usageStatsManager == null) {
            this.mEnable = false;
            Slog.w(TAG, "disable periodic for dependencies service not available");
        }
    }

    private void onBootComplete() {
        if (this.mEnable) {
            registerCloudObserver(this.mContext);
            if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_ENABLE, -2) != null) {
                this.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_ENABLE, -2));
                Slog.w(TAG, "set enable state from database: " + this.mEnable);
            }
            this.mReady = this.mEnable;
            registerCloudWhiteListObserver(this.mContext);
            if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_WHITE_LIST, -2) != null) {
                String whiteListStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_WHITE_LIST, -2);
                String[] whiteList = whiteListStr.split(",");
                for (int i = 0; i < whiteList.length; i++) {
                    if (!sCleanWhiteList.contains(whiteList[i])) {
                        sCleanWhiteList.add(whiteList[i]);
                    }
                }
                Slog.w(TAG, "set white list from database, current list: " + sCleanWhiteList);
            }
            registerCloudGameObserver(this.mContext);
            if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_GAME_ENABLE, -2) != null) {
                this.mEnableGameClean = Boolean.parseBoolean(Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_GAME_ENABLE, -2));
                Slog.w(TAG, "set game enable state from database: " + this.mEnableGameClean);
            }
        }
        if (!this.mHighDevice) {
            Message msg = this.mHandler.obtainMessage(6, CACHED_APP_MIN_ADJ, 19, "cch-empty");
            this.mHandler.sendMessageDelayed(msg, 10000L);
        }
    }

    public PeriodicCleanerService(Context context) {
        super(context);
        this.mReady = false;
        this.mHighDevice = false;
        this.mEnableFgTrim = false;
        this.mEnableGameClean = true;
        this.mEnable = SystemProperties.getBoolean(PERIODIC_ENABLE_PROP, false);
        this.mEnableStartProcess = SystemProperties.getBoolean(PERIODIC_START_PROCESS_ENABLE_PROP, false);
        this.mAppMemThreshold = SystemProperties.getInt(PERIODIC_MEM_THRES_PROP, 600);
        this.mLock = new Object();
        this.mThread = new HandlerThread(TAG);
        this.mContext = null;
        this.mBinderService = null;
        this.mLocalService = null;
        this.mHandler = null;
        this.mPMS = null;
        this.mAMS = null;
        this.mATMS = null;
        this.mWMS = null;
        this.mAudioService = null;
        this.mPKMS = null;
        this.mUSM = null;
        this.mAudioServiceClz = null;
        this.mWindowManagerInternalClz = null;
        this.mGetAllAudioFocusMethod = null;
        this.mGetVisibleWindowOwnerMethod = null;
        this.mAndroidOsDebugClz = null;
        this.mClassProcessState = null;
        this.mFieldProcessState = null;
        this.mDebugClz_Field_MEMINFO_CACHED = null;
        this.mDebugClz_Field_MEMINFO_SWAPCACHED = null;
        this.mDebugClz_Field_MEMINFO_BUFFERS = null;
        this.mDebugClz_Field_MEMINFO_SHMEM = null;
        this.mDebugClz_Field_MEMINFO_UNEVICTABLE = null;
        this.mDebugClz_MEMINFO_CACHED = 0;
        this.mDebugClz_MEMINFO_SWAPCACHED = 0;
        this.mDebugClz_MEMINFO_BUFFERS = 0;
        this.mDebugClz_MEMINFO_SHMEM = 0;
        this.mDebugClz_MEMINFO_UNEVICTABLE = 0;
        this.mIApplicationThreadClz = null;
        this.mScheduleAggressiveTrimMethod = null;
        this.mLruPackages = new ArrayList<>();
        this.mCleanHistory = new CleanInfo[HISTORY_SIZE];
        this.mLastVisibleUids = new ArrayList();
        this.mLastNonSystemFgPkg = null;
        this.mLastNonSystemFgUid = 0;
        this.mLruActiveLength = 0;
        this.mPressureTimeThreshold = new int[3];
        this.mPressureAgingThreshold = new int[3];
        this.mPressureCacheThreshold = new int[3];
        this.mLastCleanByPressure = 0L;
        this.mCleanHistoryIndex = 0;
        this.mScreenState = 3;
        this.mFgTrimTheshold = 1;
        this.mLastUpdateTime = 0L;
        this.mOverTimeAppNum = 2;
        this.mGamePlayAppNum = 2;
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.am.PeriodicCleanerService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    PeriodicCleanerService.this.mScreenState = 1;
                    PeriodicCleanerService.this.mHandler.removeMessages(2);
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    PeriodicCleanerService.this.mScreenState = 2;
                    PeriodicCleanerService.this.mHandler.sendEmptyMessageDelayed(3, AccessControlImpl.LOCK_TIME_OUT);
                }
            }
        };
        this.mContext = context;
        this.mThread.start();
        this.mHandler = new MyHandler(this.mThread.getLooper());
        init();
    }

    private void registerCloudObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.PeriodicCleanerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(PeriodicCleanerService.CLOUD_PERIODIC_ENABLE))) {
                    PeriodicCleanerService.this.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), PeriodicCleanerService.CLOUD_PERIODIC_ENABLE, -2));
                    PeriodicCleanerService periodicCleanerService = PeriodicCleanerService.this;
                    periodicCleanerService.mReady = periodicCleanerService.mEnable;
                    Slog.w(PeriodicCleanerService.TAG, "cloud control set received: " + PeriodicCleanerService.this.mEnable);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PERIODIC_ENABLE), false, observer, -2);
    }

    private void registerCloudWhiteListObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.PeriodicCleanerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(PeriodicCleanerService.CLOUD_PERIODIC_WHITE_LIST))) {
                    String whiteListStr = Settings.System.getStringForUser(context.getContentResolver(), PeriodicCleanerService.CLOUD_PERIODIC_WHITE_LIST, -2);
                    String[] whiteList = whiteListStr.split(",");
                    for (int i = 0; i < whiteList.length; i++) {
                        if (!PeriodicCleanerService.sCleanWhiteList.contains(whiteList[i])) {
                            PeriodicCleanerService.sCleanWhiteList.add(whiteList[i]);
                        }
                    }
                    Slog.w(PeriodicCleanerService.TAG, "cloud white list received, current list: " + PeriodicCleanerService.sCleanWhiteList);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PERIODIC_WHITE_LIST), false, observer, -2);
    }

    private void registerCloudGameObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.PeriodicCleanerService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(PeriodicCleanerService.CLOUD_PERIODIC_GAME_ENABLE))) {
                    PeriodicCleanerService.this.mEnableGameClean = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), PeriodicCleanerService.CLOUD_PERIODIC_GAME_ENABLE, -2));
                    Slog.w(PeriodicCleanerService.TAG, "cloud game set received: " + PeriodicCleanerService.this.mEnableGameClean);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PERIODIC_GAME_ENABLE), false, observer, -2);
    }

    private void init() {
        int index;
        if (this.mEnable) {
            boolean z = false;
            if (!dependencyCheck()) {
                this.mEnable = false;
                return;
            }
            int totalMemGB = (int) ((Process.getTotalMemory() / FormatBytesUtil.GB) + 1);
            if (totalMemGB > 4) {
                this.mHighDevice = true;
                index = totalMemGB / 2;
                this.mFgTrimTheshold = 2;
            } else {
                index = totalMemGB - 2;
                this.mFgTrimTheshold = 1;
            }
            if (index > 6) {
                index = 6;
            }
            if (!loadLruLengthConfig(index)) {
                loadDefLruLengthConfig(index);
            }
            if (!loadCacheLevelConfig(index)) {
                loadDefCacheLevelConfig(index);
            }
            loadDefTimeLevelConfig();
            this.mOverTimeAppNum = sOverTimeAppNumArray[index];
            this.mGamePlayAppNum = sGamePlayAppNumArray[index];
            if (SystemProperties.getBoolean("persist.sys.periodic.u.fgtrim", false) && checkEnableFgTrim() && totalMemGB < 8) {
                z = true;
            }
            this.mEnableFgTrim = z;
            Resources r = this.mContext.getResources();
            String[] homeOrRecentsList = r.getStringArray(285409459);
            String[] systemRelatedPkgList = r.getStringArray(285409460);
            String[] cleanWhiteList = r.getStringArray(285409457);
            String[] gameList = r.getStringArray(285409458);
            sHomeOrRecents.addAll(Arrays.asList(homeOrRecentsList));
            sSystemRelatedPkgs.addAll(Arrays.asList(systemRelatedPkgList));
            sCleanWhiteList.addAll(Arrays.asList(cleanWhiteList));
            sGameApp.addAll(Arrays.asList(gameList));
            if (DEBUG) {
                Slog.d(TAG, "HomeOrRecents pkgs: " + sHomeOrRecents + "\nSystemRelated pkgs: " + sSystemRelatedPkgs + "\nCleanWhiteList pkgs: " + sCleanWhiteList + "\nGame pkgs: " + sGameApp);
            }
        }
    }

    private boolean dependencyCheck() {
        try {
            Class<?> cls = Class.forName("com.android.server.audio.AudioService");
            this.mAudioServiceClz = cls;
            this.mGetAllAudioFocusMethod = cls.getDeclaredMethod("getAllAudioFocus", new Class[0]);
            Class<?> cls2 = Class.forName("com.android.server.wm.WindowManagerInternal");
            this.mWindowManagerInternalClz = cls2;
            this.mGetVisibleWindowOwnerMethod = cls2.getDeclaredMethod("getVisibleWindowOwner", new Class[0]);
            Class<?> cls3 = Class.forName("android.os.Debug");
            this.mAndroidOsDebugClz = cls3;
            this.mDebugClz_Field_MEMINFO_CACHED = cls3.getField("MEMINFO_CACHED");
            this.mDebugClz_Field_MEMINFO_SWAPCACHED = this.mAndroidOsDebugClz.getField("MEMINFO_SWAPCACHED");
            this.mDebugClz_Field_MEMINFO_BUFFERS = this.mAndroidOsDebugClz.getField("MEMINFO_BUFFERS");
            this.mDebugClz_Field_MEMINFO_SHMEM = this.mAndroidOsDebugClz.getField("MEMINFO_SHMEM");
            this.mDebugClz_Field_MEMINFO_UNEVICTABLE = this.mAndroidOsDebugClz.getField("MEMINFO_UNEVICTABLE");
            this.mDebugClz_MEMINFO_CACHED = this.mDebugClz_Field_MEMINFO_CACHED.getInt(null);
            this.mDebugClz_MEMINFO_SWAPCACHED = this.mDebugClz_Field_MEMINFO_SWAPCACHED.getInt(null);
            this.mDebugClz_MEMINFO_BUFFERS = this.mDebugClz_Field_MEMINFO_BUFFERS.getInt(null);
            this.mDebugClz_MEMINFO_SHMEM = this.mDebugClz_Field_MEMINFO_SHMEM.getInt(null);
            this.mDebugClz_MEMINFO_UNEVICTABLE = this.mDebugClz_Field_MEMINFO_UNEVICTABLE.getInt(null);
            Class<?> cls4 = Class.forName("com.android.internal.app.procstats.ProcessState");
            this.mClassProcessState = cls4;
            Field declaredField = cls4.getDeclaredField("mPssTable");
            this.mFieldProcessState = declaredField;
            declaredField.setAccessible(true);
            return true;
        } catch (NoSuchFieldException nfe) {
            Slog.e(TAG, "dependent field missing: " + nfe);
            return false;
        } catch (NoSuchMethodException nme) {
            Slog.e(TAG, "dependent interface missing: " + nme);
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "dependency check failed: " + e);
            return false;
        }
    }

    private boolean checkEnableFgTrim() {
        try {
            Class<?> cls = Class.forName("android.app.IApplicationThread");
            this.mIApplicationThreadClz = cls;
            this.mScheduleAggressiveTrimMethod = cls.getDeclaredMethod("scheduleAggressiveMemoryTrim", new Class[0]);
            return true;
        } catch (NoSuchMethodException nme) {
            Slog.e(TAG, "interface missing: " + nme);
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "check failed: " + e);
            return false;
        }
    }

    private int finalLruActiveLength(int length) {
        if (length <= 3) {
            return 3;
        }
        return length;
    }

    private boolean loadLruLengthConfig(int memIndex) {
        String value = SystemProperties.get("persist.sys.periodic.lru_active_length");
        String[] memArray = value.split(":");
        if (memArray.length != 7) {
            return false;
        }
        try {
            int len = Integer.parseInt(memArray[memIndex]);
            if (len <= 0) {
                return false;
            }
            this.mLruActiveLength = len;
            this.mPressureAgingThreshold[0] = finalLruActiveLength(len);
            this.mPressureAgingThreshold[1] = finalLruActiveLength((this.mLruActiveLength * 3) / 4);
            this.mPressureAgingThreshold[2] = finalLruActiveLength(this.mLruActiveLength / 2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean loadCacheLevelConfig(int memIndex) {
        String value = SystemProperties.get("persist.sys.periodic.cache_level_config");
        String[] memArray = value.split(":");
        if (memArray.length != 7 || memArray[memIndex].length() == 0) {
            return false;
        }
        String[] pressureArray = memArray[memIndex].split(",");
        if (pressureArray.length != 3) {
            return false;
        }
        int index = 0;
        for (String str : pressureArray) {
            try {
                int cache = Integer.parseInt(str);
                if (cache <= 0) {
                    return false;
                }
                this.mPressureCacheThreshold[index] = cache;
                index++;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void loadDefLruLengthConfig(int memIndex) {
        int[] iArr = sDefaultActiveLength[memIndex];
        int i = iArr[0];
        this.mLruActiveLength = i;
        int[] iArr2 = this.mPressureAgingThreshold;
        iArr2[0] = i;
        iArr2[1] = iArr[1];
        iArr2[2] = iArr[2];
    }

    private void loadDefCacheLevelConfig(int memIndex) {
        int[] iArr = this.mPressureCacheThreshold;
        int[] iArr2 = sDefaultCacheLevel[memIndex];
        iArr[0] = iArr2[0];
        iArr[1] = iArr2[1];
        iArr[2] = iArr2[2];
    }

    private void loadDefTimeLevelConfig() {
        updateTimeLevel();
        int[] iArr = this.mPressureTimeThreshold;
        int[] iArr2 = sDefaultTimeLevel;
        iArr[0] = iArr2[0];
        iArr[1] = iArr2[1];
        iArr[2] = iArr2[2];
    }

    private void updateTimeLevel() {
        String prop = SystemProperties.get(PERIODIC_TIME_THRES_PROP, (String) null);
        if (prop == null) {
            return;
        }
        String[] timeLevelString = prop.split(",");
        if (timeLevelString.length != 3) {
            return;
        }
        int[] timeLevelInt = new int[timeLevelString.length];
        int index = 0;
        for (String str : timeLevelString) {
            try {
                int time_threshold = Integer.parseInt(str);
                if (time_threshold <= 0) {
                    return;
                }
                timeLevelInt[index] = time_threshold;
                index++;
            } catch (Exception e) {
                return;
            }
        }
        System.arraycopy(timeLevelInt, 0, sDefaultTimeLevel, 0, 3);
        Slog.d(TAG, "Override mPressureTimeThreshold with: " + prop);
    }

    private void addCleanHistory(CleanInfo cInfo) {
        if (cInfo.isValid()) {
            synchronized (this.mCleanHistory) {
                CleanInfo[] cleanInfoArr = this.mCleanHistory;
                int i = this.mCleanHistoryIndex;
                cleanInfoArr[i] = cInfo;
                this.mCleanHistoryIndex = (i + 1) % HISTORY_SIZE;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEvent(MyEvent event) {
        String packageName = event.mPackage;
        if (!this.mReady || packageName == null || event.mUid == 1000 || isSystemPackage(packageName) || event.mEventType != 1) {
            return;
        }
        if (!event.mFullScreen) {
            if (DEBUG) {
                Slog.d(TAG, packageName + "/" + event.mClass + " isn't fullscreen, skip.");
            }
        } else if (!packageName.equals(this.mLastNonSystemFgPkg) || event.mUid != this.mLastNonSystemFgUid) {
            this.mLastNonSystemFgPkg = packageName;
            this.mLastNonSystemFgUid = event.mUid;
            if (updateLruPackageLocked(event.mUid, packageName) && !this.mHighDevice) {
                cleanPackageByPeriodic();
            } else {
                checkPressureAndClean("pressure");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportCleanProcess(int minAdj, int procState, String reason) {
        String longReason = "PeriodicCleaner(" + reason + ")";
        SparseArray<ArrayList<String>> killedArray = new SparseArray<>();
        synchronized (this.mAMS) {
            int N = this.mAMS.mProcessList.getLruSizeLOSP();
            for (int i = N - 2; i >= 0; i--) {
                ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i);
                if (!app.isKilledByAm() && app.getThread() != null && !app.isolated && !app.hasActivities() && !isSystemRelated(app)) {
                    ProcessStateRecord state = app.mState;
                    if (state.getCurAdj() >= minAdj && state.getCurProcState() >= procState) {
                        app.killLocked(longReason, 13, true);
                        ArrayList<String> userTargets = killedArray.get(app.userId);
                        if (userTargets == null) {
                            userTargets = new ArrayList<>();
                            killedArray.put(app.userId, userTargets);
                        }
                        userTargets.add(app.processName);
                    }
                }
            }
        }
        CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), -1, -1, reason);
        for (int i2 = 0; i2 < killedArray.size(); i2++) {
            cInfo.addCleanList(killedArray.keyAt(i2), killedArray.valueAt(i2));
        }
        addCleanHistory(cInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportMemPressure(int pressureState) {
        if (this.mReady && pressureState == 3) {
            long now = SystemClock.uptimeMillis();
            if (now - this.mLastCleanByPressure > 10000) {
                if (DEBUG) {
                    Slog.d(TAG, "try to clean for MEM_PRESSURE_HIGH.");
                }
                checkPressureAndClean("pressure");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportStartProcess(String processInfo) {
        if (this.mReady && this.mEnableStartProcess) {
            long now = SystemClock.uptimeMillis();
            int lastIndex = processInfo.lastIndexOf(":");
            String hostType = processInfo.substring(lastIndex + 1);
            if (hostType.contains("activity")) {
                String processName = processInfo.substring(0, lastIndex);
                if (this.mEnableGameClean && sGameApp.contains(processName)) {
                    if (DEBUG) {
                        Slog.d(TAG, "reportStartGame " + processName);
                    }
                    cleanPackageByGamePlay(this.mGamePlayAppNum, processName);
                } else if (sHighFrequencyApp.contains(processName) || sHighMemoryApp.contains(processName)) {
                    if (DEBUG) {
                        Slog.d(TAG, "reportStartProcess " + processName);
                    }
                    String reason = "pressure:startprocess:" + processName;
                    checkPressureAndClean(reason);
                }
            }
            if (now - this.mLastUpdateTime >= UPDATE_PROCSTATS_PERIOD) {
                updateProcStatsList();
                this.mLastUpdateTime = now;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProcStatsList() {
        updateHighMemoryAppList();
        updateHighFrequencyAppList();
        Slog.d(TAG, "update high memory and frequency app list success");
    }

    private void updateHighMemoryAppList() {
        long startTime = SystemClock.uptimeMillis();
        HashMap<String, Long> packagePss = getAllPackagePss();
        checkTime(startTime, "finish get all package pss.");
        if (packagePss == null) {
            return;
        }
        List<Map.Entry<String, Long>> listPkgPss = new ArrayList<>(packagePss.entrySet());
        listPkgPss.sort(new Comparator<Map.Entry<String, Long>>() { // from class: com.android.server.am.PeriodicCleanerService.4
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        int index = 0;
        boolean isNeedClear = true;
        for (Map.Entry<String, Long> mapping : listPkgPss) {
            if (mapping.getValue().longValue() < MAX_MEMORY_VALUE || index >= 5) {
                break;
            }
            if (isNeedClear) {
                sHighMemoryApp.clear();
                isNeedClear = false;
            }
            if (DEBUG) {
                Slog.d(TAG, "update highMemoryApp: " + mapping.getKey() + ":" + mapping.getValue());
            }
            sHighMemoryApp.add(mapping.getKey());
            index++;
        }
        Slog.d(TAG, "highmemoryapp:" + sHighMemoryApp);
    }

    private HashMap<String, Long> getAllPackagePss() {
        HashMap<String, Long> packagePss = new HashMap<>();
        try {
            ParcelFileDescriptor pfd = this.mProcessStats.getStatsOverTime(86400000 - (ProcessStats.COMMIT_PERIOD / 2));
            if (pfd == null) {
                Slog.e(TAG, "open file error.");
                return null;
            }
            ProcessStats stats = new ProcessStats(false);
            InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            stats.read(stream);
            try {
                stream.close();
            } catch (IOException e) {
            }
            if (stats.mReadError != null) {
                Slog.w(TAG, "Failure reading process stats: " + stats.mReadError);
                return null;
            }
            HashMap<String, Long> packagePss2 = getAllPackagePssDetail(stats);
            return packagePss2;
        } catch (RemoteException e2) {
            Slog.e(TAG, "RemoteException:" + e2);
            return packagePss;
        }
    }

    private HashMap<String, Long> getAllPackagePssDetail(ProcessStats stats) {
        ProcessStats.PackageState pkgState;
        int NPROCS;
        SparseArray<LongSparseArray<ProcessStats.PackageState>> uids;
        HashMap<String, Long> packagePss = new HashMap<>();
        ArrayMap<String, SparseArray<LongSparseArray<ProcessStats.PackageState>>> pkgMap = stats.mPackages.getMap();
        int NPKG = pkgMap.size();
        for (int ip = 0; ip < NPKG; ip++) {
            String pkgName = pkgMap.keyAt(ip);
            SparseArray<LongSparseArray<ProcessStats.PackageState>> uids2 = pkgMap.valueAt(ip);
            int NUID = uids2.size();
            long maxPackagePssAve = 0;
            int iu = 0;
            while (iu < NUID) {
                uids2.keyAt(iu);
                LongSparseArray<ProcessStats.PackageState> vpkgs = uids2.valueAt(iu);
                int NVERS = vpkgs.size();
                long tmpMaxPackagePssAve = 0;
                int iv = 0;
                while (iv < NVERS) {
                    vpkgs.keyAt(iv);
                    ProcessStats.PackageState pkgState2 = vpkgs.valueAt(iv);
                    ArrayMap<String, SparseArray<LongSparseArray<ProcessStats.PackageState>>> pkgMap2 = pkgMap;
                    int NPROCS2 = pkgState2.mProcesses.size();
                    long tmpMaxPackagePssAveVer = 0;
                    int NPKG2 = NPKG;
                    int NPKG3 = 0;
                    while (NPKG3 < NPROCS2) {
                        try {
                            ProcessState proc = (ProcessState) pkgState2.mProcesses.valueAt(NPKG3);
                            pkgState = pkgState2;
                            NPROCS = NPROCS2;
                            try {
                                PssTable mPssTable = (PssTable) this.mFieldProcessState.get(proc);
                                int Num = mPssTable.getKeyCount();
                                long maxSingleProcPssAve = 0;
                                int ivalue = 0;
                                while (true) {
                                    int Num2 = Num;
                                    if (ivalue >= Num2) {
                                        break;
                                    }
                                    int key = mPssTable.getKeyAt(ivalue);
                                    uids = uids2;
                                    try {
                                        if (mPssTable.getValue(key, 2) >= maxSingleProcPssAve) {
                                            maxSingleProcPssAve = mPssTable.getValue(key, 2);
                                        }
                                        ivalue++;
                                        Num = Num2;
                                        uids2 = uids;
                                    } catch (Exception e) {
                                        e = e;
                                        Slog.e(TAG, "ProcessState#mPssTable error " + e);
                                        NPKG3++;
                                        pkgState2 = pkgState;
                                        NPROCS2 = NPROCS;
                                        uids2 = uids;
                                    }
                                }
                                uids = uids2;
                                tmpMaxPackagePssAveVer += maxSingleProcPssAve;
                            } catch (Exception e2) {
                                e = e2;
                                uids = uids2;
                            }
                        } catch (Exception e3) {
                            e = e3;
                            pkgState = pkgState2;
                            NPROCS = NPROCS2;
                            uids = uids2;
                        }
                        NPKG3++;
                        pkgState2 = pkgState;
                        NPROCS2 = NPROCS;
                        uids2 = uids;
                    }
                    SparseArray<LongSparseArray<ProcessStats.PackageState>> uids3 = uids2;
                    if (tmpMaxPackagePssAveVer >= tmpMaxPackagePssAve) {
                        tmpMaxPackagePssAve = tmpMaxPackagePssAveVer;
                    }
                    iv++;
                    pkgMap = pkgMap2;
                    NPKG = NPKG2;
                    uids2 = uids3;
                }
                ArrayMap<String, SparseArray<LongSparseArray<ProcessStats.PackageState>>> pkgMap3 = pkgMap;
                int NPKG4 = NPKG;
                SparseArray<LongSparseArray<ProcessStats.PackageState>> uids4 = uids2;
                if (tmpMaxPackagePssAve >= maxPackagePssAve) {
                    maxPackagePssAve = tmpMaxPackagePssAve;
                }
                iu++;
                pkgMap = pkgMap3;
                NPKG = NPKG4;
                uids2 = uids4;
            }
            packagePss.put(pkgName, Long.valueOf(maxPackagePssAve));
        }
        return packagePss;
    }

    private void updateHighFrequencyAppList() {
        HashMap<String, Long> packageForegroundTime = getPackageForegroundTime();
        List<Map.Entry<String, Long>> packages = new ArrayList<>(packageForegroundTime.entrySet());
        packages.sort(new Comparator<Map.Entry<String, Long>>() { // from class: com.android.server.am.PeriodicCleanerService.5
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        int maxSize = packages.size();
        int maxSize2 = maxSize <= 5 ? maxSize : 5;
        sHighFrequencyApp.clear();
        for (int i = 0; i < maxSize2; i++) {
            Map.Entry<String, Long> packageName = packages.get(i);
            sHighFrequencyApp.add(packageName.getKey());
            if (DEBUG) {
                Slog.d(TAG, "update highFrequencyApp: " + packageName.getKey() + ":" + packageName.getValue());
            }
        }
    }

    private HashMap<String, Long> getPackageForegroundTime() {
        String packageName;
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 86400000;
        if (startTime < 0) {
            startTime = 0;
        }
        List<UsageStats> stats = this.mUSM.queryUsageStats(4, startTime, currentTime);
        HashMap<String, Long> packageForegroundTime = new HashMap<>();
        for (int i = 0; i < stats.size(); i++) {
            UsageStats us = stats.get(i);
            if (us != null && (packageName = us.getPackageName()) != null) {
                long totalTime = us.getTotalTimeInForeground();
                if (totalTime > 0) {
                    Long foregroundTime = packageForegroundTime.get(packageName);
                    if (foregroundTime == null) {
                        packageForegroundTime.put(packageName, Long.valueOf(totalTime));
                    } else {
                        packageForegroundTime.put(packageName, Long.valueOf(foregroundTime.longValue() + totalTime));
                    }
                }
            }
        }
        return packageForegroundTime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenOff() {
        if (this.mScreenState != 2) {
            Slog.d(TAG, "screen on when deap clean, skip");
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        List<ProcessRecord> victimList = new ArrayList<>();
        List<String> killed = new ArrayList<>();
        List<Integer> topUids = new ArrayList<>();
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            int i = N - 1;
            for (int j = 0; i >= 0 && j < 2; j++) {
                topUids.add(Integer.valueOf(this.mLruPackages.get(i).mUid));
                i--;
            }
        }
        this.mLastVisibleUids.remove(SYSTEM_UID_OBJ);
        List<Integer> audioActiveUids = getAudioActiveUids();
        List<Integer> locationActiveUids = getLocationActiveUids();
        if (DEBUG) {
            Slog.d(TAG, "TopPkg=" + topUids + ", AudioActive=" + audioActiveUids + ", LocationActive=" + locationActiveUids + ", LastVisible=" + this.mLastVisibleUids);
        }
        synchronized (this.mAMS) {
            int N2 = this.mAMS.mProcessList.getLruSizeLOSP();
            int i2 = N2 - 1;
            while (i2 >= 0) {
                ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i2);
                int i3 = i2;
                if (app.mProfile.getLastPss() / FormatBytesUtil.KB > this.mAppMemThreshold && app.mProfile.getPid() != ActivityManagerService.MY_PID && !audioActiveUids.contains(Integer.valueOf(app.uid)) && !locationActiveUids.contains(Integer.valueOf(app.uid)) && !topUids.contains(Integer.valueOf(app.uid))) {
                    victimList.add(app);
                }
                i2 = i3 - 1;
            }
            int i4 = 0;
            while (true) {
                if (i4 >= victimList.size()) {
                    break;
                }
                if (this.mScreenState != 2) {
                    Slog.w(TAG, "screen on when deap clean, abort");
                    break;
                }
                ProcessRecord app2 = victimList.get(i4);
                Slog.w(TAG, "Kill process " + app2.processName + ", pid " + app2.mProfile.getPid() + ", pss " + app2.mProfile.getLastPss() + " for abnormal mem usage.");
                app2.killLocked("deap clean " + app2.mProfile.getLastPss(), 5, true);
                killed.add(app2.processName);
                i4++;
            }
        }
        if (killed.size() > 0) {
            CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), -1, 2, "deap");
            cInfo.addCleanList(0, killed);
            addCleanHistory(cInfo);
        }
        checkTime(startTime, "finish deap clean");
    }

    public boolean isPreviousApp(String packageName) {
        long curTime = System.currentTimeMillis();
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            if (N >= 2) {
                PackageUseInfo previousApp = this.mLruPackages.get(N - 2);
                if (previousApp.mPackageName.equals(packageName) && curTime - previousApp.mBackgroundTime <= 300000) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean updateLruPackageLocked(int uid, String packageName) {
        int newFgIndex = -1;
        int userId = UserHandle.getUserId(uid);
        PackageUseInfo info = null;
        long backgroundTime = System.currentTimeMillis();
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            int i = N - 2;
            while (true) {
                if (i < 0) {
                    break;
                }
                info = this.mLruPackages.get(i);
                if (info.mUserId != userId || !info.mPackageName.equals(packageName)) {
                    i--;
                } else {
                    this.mLruPackages.remove(info);
                    info.mBackgroundTime = 0L;
                    info.mFgTrimDone = false;
                    this.mLruPackages.add(info);
                    newFgIndex = i;
                    PackageUseInfo modifyInfo = this.mLruPackages.get(N - 2);
                    modifyInfo.mBackgroundTime = backgroundTime;
                    break;
                }
            }
            if (newFgIndex == -1) {
                PackageUseInfo newInfo = obtainPackageUseInfo(uid, packageName, 0L);
                this.mLruPackages.add(newInfo);
                int N2 = this.mLruPackages.size();
                if (N2 >= 2) {
                    PackageUseInfo modifyInfo2 = this.mLruPackages.get(N2 - 2);
                    modifyInfo2.mBackgroundTime = backgroundTime;
                }
                return true;
            }
            if (info != null && info.mUid != uid) {
                Slog.d(TAG, packageName + " re-installed, NewAppId=" + UserHandle.getAppId(uid) + ", OldAppId=" + UserHandle.getAppId(info.mUid));
                info.updateUid(uid);
            }
            return N - newFgIndex > this.mLruActiveLength;
        }
    }

    private PackageUseInfo obtainPackageUseInfo(int uid, String packageName, long backgroundTime) {
        return new PackageUseInfo(uid, packageName, backgroundTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanPackageByTime(int pressure) {
        doClean(this.mLruActiveLength, 100, pressure, "time");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanPackageByPeriodic() {
        doClean(this.mLruActiveLength, 100, -1, "cycle");
    }

    private void cleanPackageByGamePlay(int activeNum, String startProcessName) {
        String reason = "game:" + startProcessName;
        doClean(activeNum, 100, -1, reason);
        List<Integer> gamePackagesUid = findOtherGamePackageLocked(activeNum, startProcessName);
        if (gamePackagesUid.size() == 0) {
            return;
        }
        String longReason = "PeriodicCleaner(" + activeNum + "|-1|muti_game)";
        SparseArray<ArrayList<String>> killedArray = new SparseArray<>();
        Iterator<Integer> it = gamePackagesUid.iterator();
        while (it.hasNext()) {
            int uid = it.next().intValue();
            List<ProcessRecord> records = this.mPMS.getProcessRecordByUid(uid);
            if (records != null) {
                for (ProcessRecord app : records) {
                    if (!app.isKilledByAm() && app.getThread() != null && !app.isolated && !isSystemRelated(app)) {
                        this.mPMS.getProcessKiller().killApplication(app, longReason, false);
                        ArrayList<String> userTargets = killedArray.get(app.userId);
                        if (userTargets == null) {
                            userTargets = new ArrayList<>();
                            killedArray.put(app.userId, userTargets);
                        }
                        userTargets.add(app.processName);
                    }
                }
            }
        }
        CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), -1, -1, "muti_game");
        for (int i = 0; i < killedArray.size(); i++) {
            cInfo.addCleanList(killedArray.keyAt(i), killedArray.valueAt(i));
        }
        addCleanHistory(cInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanPackageByPressure(int pressure, String reason) {
        doClean(this.mPressureAgingThreshold[pressure], 100, pressure, reason);
        reCheckPressureAndClean();
    }

    private void reCheckPressureAndClean() {
        int pressure = getMemPressureLevel();
        if (pressure != -1) {
            cleanPackageByTime(pressure);
        }
    }

    private void checkPressureAndClean(String reason) {
        int pressure = getMemPressureLevel();
        if (pressure != -1) {
            cleanPackageByPressure(pressure, reason);
            this.mLastCleanByPressure = SystemClock.uptimeMillis();
        }
    }

    private void doFgTrim(int threshold) {
        long startTime = SystemClock.uptimeMillis();
        List<Integer> visibleUids = getVisibleWindowOwner();
        checkTime(startTime, "finish doFgTrim#getVisibleWindowOwner");
        HashMap<Integer, PackageUseInfo> trimTargets = new HashMap<>();
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            for (int i = (N - 1) - threshold; i >= 0; i--) {
                PackageUseInfo info = this.mLruPackages.get(i);
                if (!info.mFgTrimDone) {
                    trimTargets.put(Integer.valueOf(info.mUid), info);
                }
            }
        }
        if (trimTargets.size() <= 0) {
            return;
        }
        checkTime(startTime, "finish doFgTrim#getTrimTargets");
        synchronized (this.mAMS) {
            int N2 = this.mAMS.mProcessList.getLruSizeLOSP();
            for (int i2 = N2 - 1; i2 >= 0; i2--) {
                ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i2);
                if (!app.isKilledByAm() && app.getThread() != null) {
                    if (!app.isolated && !visibleUids.contains(Integer.valueOf(app.uid))) {
                        PackageUseInfo target = trimTargets.get(Integer.valueOf(app.uid));
                        if (target != null) {
                            try {
                                this.mScheduleAggressiveTrimMethod.invoke(app.getThread(), new Object[0]);
                                target.mFgTrimDone = true;
                                Slog.d(TAG, "send aggressive trim to " + app);
                            } catch (Exception e) {
                                Slog.e(TAG, "doFgTrim#scheduleAggressiveMemoryTrim: " + e);
                            }
                        }
                    }
                    if (DEBUG) {
                        Slog.d(TAG, app + " is isolated or uid has visible window.");
                    }
                }
            }
            checkTime(startTime, "finish doFgTrim");
        }
    }

    private int getMemPressureLevel() {
        int level;
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long[] rawInfo = minfo.getRawInfo();
        long j = rawInfo[this.mDebugClz_MEMINFO_CACHED];
        int i = this.mDebugClz_MEMINFO_SWAPCACHED;
        long otherFile = j + rawInfo[i] + rawInfo[this.mDebugClz_MEMINFO_BUFFERS];
        long needRemovedFile = rawInfo[this.mDebugClz_MEMINFO_SHMEM] + rawInfo[this.mDebugClz_MEMINFO_UNEVICTABLE] + rawInfo[i];
        if (otherFile > needRemovedFile) {
            otherFile -= needRemovedFile;
        }
        int[] iArr = this.mPressureCacheThreshold;
        if (otherFile >= iArr[0]) {
            level = -1;
        } else if (otherFile >= iArr[1]) {
            level = 0;
        } else if (otherFile >= iArr[2]) {
            level = 1;
        } else {
            level = 2;
        }
        if (DEBUG) {
            Slog.i(TAG, "Other File: " + otherFile + "KB. Mem Pressure Level: " + level);
        }
        return level;
    }

    private void doClean(int thresHold, int killLevel, int pressure, String reason) {
        SparseArray<ArrayList<String>> agingPkgs;
        String reason2;
        long startTime = SystemClock.uptimeMillis();
        if (reason.equals("time")) {
            SparseArray<ArrayList<String>> agingPkgs2 = findOverTimePackageLocked(pressure);
            agingPkgs = agingPkgs2;
        } else {
            SparseArray<ArrayList<String>> agingPkgs3 = findAgingPackageLocked(thresHold);
            agingPkgs = agingPkgs3;
        }
        int processIndex = reason.contains("game:") ? 4 : -1;
        if (reason.contains("pressure:startprocess:")) {
            processIndex = 21;
        }
        int processIndex2 = processIndex;
        if (processIndex2 <= -1) {
            reason2 = reason;
        } else {
            String currentPackage = reason.substring(processIndex2 + 1);
            excludeCurrentProcess(agingPkgs, currentPackage);
            reason2 = reason.substring(0, processIndex2);
        }
        String longReason = "PeriodicCleaner(" + thresHold + "|" + pressure + "|" + reason2 + ")";
        List<Integer> visibleUids = getVisibleWindowOwner();
        List<Integer> audioActiveUids = getAudioActiveUids();
        List<Integer> locationActiveUids = getLocationActiveUids();
        checkTime(startTime, "finish get active and visible uids");
        if (DEBUG) {
            Slog.d(TAG, "AgingPacakges: " + agingPkgs + ", LocationActiveUids: " + locationActiveUids + ", VisibleUids: " + visibleUids + ", AudioActiveUids: " + audioActiveUids);
        }
        CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), pressure, thresHold, reason2);
        int i = 0;
        while (i < agingPkgs.size()) {
            int userId = agingPkgs.keyAt(i);
            int i2 = i;
            long startTime2 = startTime;
            CleanInfo cInfo2 = cInfo;
            ArrayList<String> userTargets = filterOutKillablePackages(userId, pressure, agingPkgs.valueAt(i), visibleUids, audioActiveUids, locationActiveUids);
            if (userTargets.size() > 0) {
                killTargets(userId, userTargets, killLevel, longReason);
                cInfo2.addCleanList(userId, userTargets);
            }
            i = i2 + 1;
            cInfo = cInfo2;
            startTime = startTime2;
        }
        addCleanHistory(cInfo);
    }

    private SparseArray<ArrayList<String>> findOverTimePackageLocked(int pressure) {
        SparseArray<ArrayList<String>> agingPkgs = new SparseArray<>();
        synchronized (this.mLock) {
            int size = this.mLruPackages.size() - this.mOverTimeAppNum;
            if (size < 0) {
                return agingPkgs;
            }
            PackageUseInfo tmpInfo = this.mLruPackages.get(size);
            for (int i = 0; i < size; i++) {
                PackageUseInfo histroyInfo = this.mLruPackages.get(i);
                long diffTime = tmpInfo.mBackgroundTime - histroyInfo.mBackgroundTime;
                if (DEBUG) {
                    Slog.d(TAG, "OverTimeValue:" + tmpInfo.mBackgroundTime + "-" + histroyInfo.mBackgroundTime + "=" + diffTime + "?" + sDefaultTimeLevel[pressure]);
                    Slog.d(TAG, "OverTimePackage:" + tmpInfo.mPackageName + ":" + histroyInfo.mPackageName);
                }
                if (diffTime < sDefaultTimeLevel[pressure]) {
                    break;
                }
                ArrayList<String> userTargets = agingPkgs.get(histroyInfo.mUserId);
                if (userTargets == null) {
                    userTargets = new ArrayList<>();
                    agingPkgs.put(histroyInfo.mUserId, userTargets);
                }
                userTargets.add(histroyInfo.mPackageName);
            }
            return agingPkgs;
        }
    }

    private SparseArray<ArrayList<String>> findAgingPackageLocked(int threshold) {
        SparseArray<ArrayList<String>> agingPkgs = new SparseArray<>();
        synchronized (this.mLock) {
            long startTime = SystemClock.uptimeMillis();
            int size = this.mLruPackages.size() - threshold;
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    PackageUseInfo info = this.mLruPackages.get(i);
                    ArrayList<String> userTargets = agingPkgs.get(info.mUserId);
                    if (userTargets == null) {
                        userTargets = new ArrayList<>();
                        agingPkgs.put(info.mUserId, userTargets);
                    }
                    userTargets.add(info.mPackageName);
                }
            }
            checkTime(startTime, "finish findAgingPackageLocked");
        }
        return agingPkgs;
    }

    private List<Integer> findOtherGamePackageLocked(int threshold, String startProcessName) {
        long startTime = SystemClock.uptimeMillis();
        List<PackageUseInfo> lruPrevPackages = null;
        List<Integer> gamePackagesUid = new ArrayList<>();
        synchronized (this.mLock) {
            int lruSize = this.mLruPackages.size();
            if (lruSize > 0) {
                int end = this.mLruPackages.size();
                int start = this.mLruPackages.size() - threshold;
                if (start < 0) {
                    start = 0;
                }
                lruPrevPackages = this.mLruPackages.subList(start, end);
            }
        }
        if (lruPrevPackages != null) {
            for (PackageUseInfo info : lruPrevPackages) {
                if (sGameApp.contains(info.mPackageName) && !startProcessName.equals(info.mPackageName)) {
                    gamePackagesUid.add(Integer.valueOf(info.mUid));
                }
            }
        }
        checkTime(startTime, "finish findOtherGamePackageLocked");
        return gamePackagesUid;
    }

    private void excludeCurrentProcess(SparseArray<ArrayList<String>> agingPkgs, String currentPackage) {
        if (agingPkgs == null || currentPackage == null) {
            return;
        }
        for (int i = 0; i < agingPkgs.size(); i++) {
            ArrayList<String> pkgs = agingPkgs.get(i);
            if (pkgs != null) {
                pkgs.remove(currentPackage);
            }
        }
    }

    private List<Integer> getAudioActiveUids() {
        List<AudioPlaybackConfiguration> activePlayers = this.mAudioService.getActivePlaybackConfigurations();
        List<Integer> activeUids = new ArrayList<>();
        for (AudioPlaybackConfiguration conf : activePlayers) {
            int state = conf.getPlayerState();
            if (state == 2 || state == 3) {
                activeUids.add(Integer.valueOf(conf.getClientUid()));
            }
        }
        List<Integer> focusUids = getAllAudioFocus();
        for (Integer uid : focusUids) {
            if (!activeUids.contains(uid)) {
                activeUids.add(uid);
            }
        }
        if (DEBUG && activeUids.size() > 0) {
            int[] uids = new int[activeUids.size()];
            for (int i = 0; i < activeUids.size(); i++) {
                uids[i] = activeUids.get(i).intValue();
            }
        }
        return activeUids;
    }

    private List<Integer> getAllAudioFocus() {
        try {
            return (List) this.mGetAllAudioFocusMethod.invoke(this.mAudioService, new Object[0]);
        } catch (Exception e) {
            Slog.e(TAG, "getAllAudioFocus: " + e);
            return new ArrayList();
        }
    }

    private List<Integer> getVisibleWindowOwner() {
        try {
            List<Integer> list = (List) this.mGetVisibleWindowOwnerMethod.invoke(this.mWMS, new Object[0]);
            this.mLastVisibleUids = list;
            return list;
        } catch (Exception e) {
            Slog.e(TAG, "getVisibleWindowOwner: " + e);
            return new ArrayList();
        }
    }

    private List<Integer> getLocationActiveUids() {
        List<Integer> uids = new ArrayList<>();
        List<ProcessPolicy.ActiveUidRecord> activeUids = this.mPMS.getProcessPolicy().getActiveUidRecordList(3);
        if (activeUids != null) {
            for (ProcessPolicy.ActiveUidRecord r : activeUids) {
                int uid = r.uid;
                if (!uids.contains(Integer.valueOf(uid))) {
                    uids.add(Integer.valueOf(uid));
                }
            }
        }
        return uids;
    }

    private ArrayList<String> filterOutKillablePackages(int userId, int pressure, ArrayList<String> agingPkgs, List<Integer> visibleUids, List<Integer> audioActiveUids, List<Integer> locationActiveUids) {
        int N;
        int i;
        ActivityManagerService activityManagerService;
        long startTime = SystemClock.uptimeMillis();
        HashMap<String, Boolean> dynWhitelist = this.mPMS.getProcessPolicy().updateDynamicWhiteList(this.mContext, userId);
        checkTime(startTime, "finish updateDynamicWhiteList");
        ArrayList<String> killList = new ArrayList<>();
        boolean isCameraForeground = isCameraForegroundCase();
        ActivityManagerService activityManagerService2 = this.mAMS;
        synchronized (activityManagerService2) {
            try {
                try {
                    int N2 = this.mAMS.mProcessList.getLruSizeLOSP();
                    int i2 = N2 - 1;
                    while (i2 >= 0) {
                        ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i2);
                        if (app.isKilledByAm() || app.isKilled() || app.getThread() == null || app.userId != userId) {
                            N = N2;
                            i = i2;
                            activityManagerService = activityManagerService2;
                        } else if (!agingPkgs.contains(app.info.packageName)) {
                            N = N2;
                            i = i2;
                            activityManagerService = activityManagerService2;
                        } else if (app.isolated) {
                            N = N2;
                            i = i2;
                            activityManagerService = activityManagerService2;
                        } else {
                            N = N2;
                            i = i2;
                            activityManagerService = activityManagerService2;
                            if (canCleanPackage(app, pressure, dynWhitelist, visibleUids, audioActiveUids, locationActiveUids, isCameraForeground)) {
                                if (!killList.contains(app.info.packageName)) {
                                    killList.add(app.info.packageName);
                                }
                            } else {
                                agingPkgs.remove(app.info.packageName);
                                killList.remove(app.info.packageName);
                            }
                        }
                        i2 = i - 1;
                        N2 = N;
                        activityManagerService2 = activityManagerService;
                    }
                    ActivityManagerService activityManagerService3 = activityManagerService2;
                    checkTime(startTime, "finish filterOutKillablePackages");
                    return killList;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                ActivityManagerService activityManagerService4 = activityManagerService2;
                throw th;
            }
        }
    }

    private boolean isCameraForegroundCase() {
        ActivityTaskManager.RootTaskInfo info;
        try {
            info = this.mATMS.getFocusedRootTaskInfo();
        } catch (RemoteException e) {
        }
        if (info != null && info.topActivity != null) {
            String packageName = info.topActivity.getPackageName();
            if (DEBUG) {
                Slog.i(TAG, "current focused package: " + packageName);
            }
            if (!"com.android.camera".equals(packageName)) {
                return false;
            }
            return true;
        }
        Slog.e(TAG, "get getFocusedStackInfo error.");
        return false;
    }

    private boolean canCleanPackage(ProcessRecord app, int pressure, HashMap<String, Boolean> dynWhitelist, List<Integer> visibleUids, List<Integer> activeAudioUids, List<Integer> locationActiveUids, boolean isCameraForeground) {
        String packageName = app.info.packageName;
        if (isSystemRelated(app) || isDynWhitelist(packageName, dynWhitelist) || isActive(app, visibleUids, activeAudioUids, locationActiveUids) || isImportant(app, pressure)) {
            return false;
        }
        return (isCameraForeground && isSkipClean(app, pressure)) ? false : true;
    }

    public boolean isSystemRelated(ProcessRecord app) {
        String packageName = app.info.packageName;
        if (isSystemPackage(packageName) || isWhiteListPackage(packageName) || !Process.isApplicationUid(app.info.uid)) {
            if (DEBUG) {
                Slog.d(TAG, packageName + " is in exclude list.");
            }
            return true;
        }
        if (packageName != null && packageName.contains("com.google.android")) {
            if (DEBUG) {
                Slog.d(TAG, packageName + ", skip for google.");
            }
            return true;
        }
        return false;
    }

    private boolean isSkipClean(ProcessRecord app, int pressure) {
        if (pressure == 2 && app.mState.getCurAdj() >= CACHED_APP_MIN_ADJ) {
            return false;
        }
        return true;
    }

    private boolean isActive(ProcessRecord app, List<Integer> visibleUids, List<Integer> activeAudioUids, List<Integer> locationActiveUids) {
        String str = app.info.packageName;
        if (app.mState.getCurAdj() < 0 || visibleUids.contains(Integer.valueOf(app.uid))) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " is persistent or owning visible window.");
            }
            return true;
        }
        if (locationActiveUids.contains(Integer.valueOf(app.uid)) || activeAudioUids.contains(Integer.valueOf(app.uid))) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " is audio or gps active.");
            }
            return true;
        }
        return false;
    }

    private boolean isDynWhitelist(String packageName, HashMap<String, Boolean> dynWhitelist) {
        if (dynWhitelist.containsKey(packageName)) {
            if (DEBUG) {
                Slog.d(TAG, packageName + " is dynamic whitelist.");
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isImportant(ProcessRecord app, int pressure) {
        if (pressure <= 0 && app.mServices.hasForegroundServices()) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " has ForegroundService.");
            }
            return true;
        }
        if (pressure <= 1 && ProcessManager.isLockedApplication(app.info.packageName, app.userId)) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " isLocked.");
            }
            return true;
        }
        if ((app.mState.getAdjSource() instanceof ProcessRecord) && ((ProcessRecord) app.mState.getAdjSource()).uid == this.mLastNonSystemFgUid) {
            if (DEBUG) {
                Slog.d(TAG, "Last top pkg " + this.mLastNonSystemFgPkg + " depend " + app);
            }
            return true;
        }
        return false;
    }

    private void killTargets(int userId, ArrayList<String> targets, int killLevel, String reason) {
        if (targets.size() <= 0) {
            return;
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            ArrayMap<Integer, List<String>> killList = new ArrayMap<>();
            killList.put(Integer.valueOf(killLevel), targets);
            ProcessConfig config = new ProcessConfig(10, userId, killList, reason);
            this.mPMS.kill(config);
            if (DEBUG) {
                Slog.d(TAG, "User" + userId + ": Clean " + targets.toString() + " for " + reason);
            }
            checkTime(startTime, "finish clean victim packages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > (DEBUG ? 50 : 200)) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpFgLru(PrintWriter pw) {
        pw.println("\n---- Foreground-LRU ----");
        int curPressure = getMemPressureLevel();
        synchronized (this.mLock) {
            pw.println("Settings:");
            pw.print("  Enable=" + this.mEnable);
            pw.print(" Ready=" + this.mReady);
            pw.print(" FgTrim=" + this.mEnableFgTrim);
            pw.print(" Debug=" + DEBUG);
            if (this.mEnableFgTrim) {
                pw.print(" FgTrimThreshold=" + this.mFgTrimTheshold);
            }
            pw.print(" mOverTimeAppNum=" + this.mOverTimeAppNum);
            pw.print(" mGamePlayAppNum=" + this.mGamePlayAppNum);
            pw.println(" LruLengthLimit=" + this.mLruActiveLength);
            pw.print("  AvailableLow=" + this.mPressureCacheThreshold[0] + "Kb");
            pw.print(" AvailableMin=" + this.mPressureCacheThreshold[1] + "Kb");
            pw.println(" AvailableCritical=" + this.mPressureCacheThreshold[2] + "Kb");
            pw.println("Package Usage LRU:");
            pw.println("  CurLength:" + this.mLruPackages.size() + " CurrentPressure:" + pressureToString(curPressure));
            for (int i = this.mLruPackages.size() - 1; i >= 0; i--) {
                pw.println("  " + this.mLruPackages.get(i).toString());
            }
            pw.println("High Memory Usage List:");
            for (String str : sHighMemoryApp) {
                pw.println("  " + str);
            }
            pw.println("High Frequency Usage List:");
            for (String str2 : sHighFrequencyApp) {
                pw.println("  " + str2);
            }
        }
        pw.println("---- End of Foreground-LRU ----");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpCleanHistory(PrintWriter pw) {
        pw.println("\n---- CleanHistory ----");
        synchronized (this.mCleanHistory) {
            int index = this.mCleanHistoryIndex - 1;
            SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
            int i = 0;
            while (true) {
                int i2 = HISTORY_SIZE;
                if (i >= i2) {
                    break;
                }
                int index2 = (index + i2) % i2;
                if (this.mCleanHistory[index2] == null) {
                    break;
                }
                pw.print("#" + i);
                pw.print(" " + formater.format(new Date(this.mCleanHistory[index2].mCleanTime)));
                pw.print(" " + pressureToString(this.mCleanHistory[index2].mPressure));
                pw.print(" " + this.mCleanHistory[index2].mAgingThresHold);
                pw.print(" " + this.mCleanHistory[index2].mReason);
                pw.println(" " + this.mCleanHistory[index2].mCleanList);
                index = index2 - 1;
                i++;
            }
        }
        pw.println("---- End of CleanHistory ----");
    }

    private String pressureToString(int pressure) {
        switch (pressure) {
            case -1:
                return EdgeSuppressionFactory.TYPE_NORMAL;
            case 0:
                return "low";
            case 1:
                return "min";
            case 2:
                return "critical";
            default:
                return "unkown";
        }
    }

    private static boolean isHomeOrRecents(String packageName) {
        return sHomeOrRecents.contains(packageName);
    }

    private static boolean isSystemPackage(String packageName) {
        return sSystemRelatedPkgs.contains(packageName) || isHomeOrRecents(packageName);
    }

    private static boolean isWhiteListPackage(String packageName) {
        return sCleanWhiteList.contains(packageName);
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PeriodicCleanerService.this.mContext, PeriodicCleanerService.TAG, pw)) {
                PeriodicCleanerService.this.dumpFgLru(pw);
                PeriodicCleanerService.this.dumpCleanHistory(pw);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            PeriodicCleanerService periodicCleanerService = PeriodicCleanerService.this;
            new PeriodicShellCmd(periodicCleanerService).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService implements PeriodicCleanerInternalStub {
        private LocalService() {
        }

        public void reportEvent(String packageName, String className, int uid, int eventType, boolean fullscreen) {
            MyEvent event = new MyEvent(packageName, className, uid, eventType, fullscreen);
            PeriodicCleanerService.this.mHandler.obtainMessage(1, event).sendToTarget();
        }

        public void reportMemPressure(int pressureState) {
            PeriodicCleanerService.this.mHandler.obtainMessage(2, pressureState, 0).sendToTarget();
        }

        public void reportStartProcess(String processName, String hostType) {
            String processInfo = processName + ":" + hostType;
            PeriodicCleanerService.this.mHandler.obtainMessage(5, processInfo).sendToTarget();
        }

        public void enablePeriodicOrNot(boolean periodicState) {
            PeriodicCleanerService.this.mReady = periodicState;
            if (PeriodicCleanerService.DEBUG) {
                Slog.i(PeriodicCleanerService.TAG, "set periodicCleanerPolicy State to " + periodicState);
            }
        }
    }

    /* loaded from: classes.dex */
    private class PeriodicShellCmd extends ShellCommand {
        PeriodicCleanerService mService;

        public PeriodicShellCmd(PeriodicCleanerService service) {
            this.mService = service;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case -1298848381:
                        if (cmd.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -295215897:
                        if (cmd.equals("updatelist")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3095028:
                        if (cmd.equals("dump")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 94746185:
                        if (cmd.equals("clean")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 95458899:
                        if (cmd.equals("debug")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
            } catch (Exception e) {
                pw.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e(PeriodicCleanerService.TAG, "Error running shell command", e);
            }
            switch (c) {
                case 0:
                    this.mService.dumpFgLru(pw);
                    this.mService.dumpCleanHistory(pw);
                    return 0;
                case 1:
                    runClean(pw);
                    return 0;
                case 2:
                    boolean enable = Boolean.parseBoolean(getNextArgRequired());
                    this.mService.mEnable = enable;
                    this.mService.mReady = enable;
                    pw.println("periodic cleaner enabled: " + enable);
                    return 0;
                case 3:
                    boolean debug = Boolean.parseBoolean(getNextArgRequired());
                    PeriodicCleanerService.DEBUG = debug;
                    pw.println("periodic cleaner debug enabled: " + debug);
                    return 0;
                case 4:
                    PeriodicCleanerService.this.updateProcStatsList();
                    pw.println("periodic cleaner update list");
                    return 0;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        private void runClean(PrintWriter pw) {
            char c;
            int pressure;
            String opt = getNextOption();
            if (opt == null) {
                pw.println("trigger clean package by periodic");
                PeriodicCleanerService.this.cleanPackageByPeriodic();
                return;
            }
            switch (opt.hashCode()) {
                case 1509:
                    if (opt.equals("-r")) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    String reason = getNextArgRequired();
                    if ("periodic".equals(reason)) {
                        PeriodicCleanerService.this.cleanPackageByPeriodic();
                        return;
                    }
                    if ("pressure-low".equals(reason)) {
                        pressure = 0;
                    } else if ("pressure-min".equals(reason)) {
                        pressure = 1;
                    } else if ("pressure-critical".equals(reason)) {
                        pressure = 2;
                    } else {
                        pw.println("error: invalid reason: " + reason);
                        return;
                    }
                    if (peekNextArg() != null) {
                        String reasonTime = getNextArgRequired();
                        if ("time".equals(reasonTime)) {
                            pw.println("trigger clean package by " + reason + "-" + reasonTime);
                            PeriodicCleanerService.this.cleanPackageByTime(pressure);
                            return;
                        } else {
                            pw.println("error: invalid option: " + opt);
                            onHelp();
                            return;
                        }
                    }
                    pw.println("trigger clean package by " + reason);
                    PeriodicCleanerService.this.cleanPackageByPressure(pressure, "debug-pressure");
                    return;
                default:
                    pw.println("error: invalid option: " + opt);
                    onHelp();
                    return;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Periodic Cleaner commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("");
            pw.println("  dump");
            pw.println("    Print fg-lru and clean history.");
            pw.println("");
            pw.println("  clean [-r REASON]");
            pw.println("    Trigger clean action.");
            pw.println("      -r: select clean reason");
            pw.println("          REASON is one of:");
            pw.println("            periodic");
            pw.println("            pressure-low");
            pw.println("            pressure-min");
            pw.println("            pressure-critical");
            pw.println("            pressure-low time");
            pw.println("            pressure-min time");
            pw.println("            pressure-critical time");
            pw.println("          default reason is periodic if no REASON");
            pw.println("");
            pw.println("  enable [true|false]");
            pw.println("    Enable/Disable peridic cleaner.");
            pw.println("");
            pw.println("  debug [true|false]");
            pw.println("    Enable/Disable debug config.");
            pw.println("");
            pw.println("  updatelist");
            pw.println("    Update highfrequencyapp and highmemoryapp list.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageUseInfo {
        private long mBackgroundTime;
        private boolean mFgTrimDone = false;
        private String mPackageName;
        private int mUid;
        private int mUserId;

        public PackageUseInfo(int uid, String packageName, long backgroundTime) {
            this.mPackageName = packageName;
            this.mUserId = UserHandle.getUserId(uid);
            this.mUid = uid;
            this.mBackgroundTime = backgroundTime;
        }

        public String toString() {
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "u" + this.mUserId + "/" + UserHandle.getAppId(this.mUid) + ":" + this.mPackageName + ":" + (this.mFgTrimDone ? "(trimed)" : "(non-trimed)") + ":" + dateformat.format(Long.valueOf(this.mBackgroundTime));
        }

        public void updateUid(int uid) {
            this.mUid = uid;
            this.mUserId = UserHandle.getUserId(uid);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PackageUseInfo) {
                PackageUseInfo another = (PackageUseInfo) obj;
                if (this.mUid == another.mUid && this.mPackageName.equals(another.mPackageName)) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj instanceof MyEvent) {
                        PeriodicCleanerService.this.reportEvent((MyEvent) msg.obj);
                        return;
                    }
                    return;
                case 2:
                    PeriodicCleanerService.this.reportMemPressure(msg.arg1);
                    return;
                case 3:
                    PeriodicCleanerService.this.handleScreenOff();
                    return;
                case 4:
                default:
                    return;
                case 5:
                    PeriodicCleanerService.this.reportStartProcess((String) msg.obj);
                    return;
                case 6:
                    PeriodicCleanerService.this.reportCleanProcess(msg.arg1, msg.arg2, (String) msg.obj);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CleanInfo {
        private int mAgingThresHold;
        private long mCleanTime;
        private int mPressure;
        private String mReason;
        private boolean mValid = false;
        private SparseArray<List<String>> mCleanList = new SparseArray<>();

        CleanInfo(long currentTime, int pressure, int agingThresHold, String reason) {
            this.mCleanTime = currentTime;
            this.mPressure = pressure;
            this.mAgingThresHold = agingThresHold;
            this.mReason = reason;
        }

        public void addCleanList(int userId, List<String> list) {
            if (this.mCleanList.get(userId) != null) {
                Slog.e(PeriodicCleanerService.TAG, "Already exists old mapping for user " + userId);
            }
            this.mCleanList.put(userId, list);
            this.mValid = true;
        }

        public boolean isValid() {
            return this.mValid;
        }

        public String toString() {
            return "[" + this.mCleanTime + ": " + this.mCleanList + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyEvent {
        public String mClass;
        public int mEventType;
        public boolean mFullScreen;
        public String mPackage;
        public int mUid;

        public MyEvent(String packageName, String className, int uid, int event, boolean fullscreen) {
            this.mPackage = packageName;
            this.mClass = className;
            this.mEventType = event;
            this.mFullScreen = fullscreen;
            this.mUid = uid;
        }
    }

    public static boolean isHomeOrRecentsToKeepAlive(String packageName) {
        return sHomeOrRecents.contains(packageName);
    }
}
