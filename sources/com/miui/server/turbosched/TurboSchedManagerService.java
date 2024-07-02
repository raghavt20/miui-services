package com.miui.server.turbosched;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.NativeTurboSchedManager;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.TurboSchedMonitor;
import android.provider.Settings;
import android.util.LocalLog;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.am.ProcessUtils;
import com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda1;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import miui.turbosched.ITurboSchedManager;

/* loaded from: classes.dex */
public class TurboSchedManagerService extends ITurboSchedManager.Stub {
    private static final String BOARD_TEMP_FILE = "/sys/class/thermal/thermal_message/board_sensor_temp";
    public static final String BOOST_ENABLE_PATH = "/sys/module/metis/parameters/mi_fboost_enable";
    public static final String BOOST_SCHED_PATH = "/sys/module/metis/parameters/boost_task";
    public static final String BOOST_THERMAL_PATH = "/sys/class/thermal/thermal_message/boost";
    private static final String CLOUD_TURBO_SCHED_ALLOW_LIST = "cloud_turbo_sched_allow_list";
    private static final String CLOUD_TURBO_SCHED_DPS_ENABLE = "cloud_turbo_sched_dps_enable";
    private static final String CLOUD_TURBO_SCHED_ENABLE_ALL = "cloud_turbo_sched_enable";
    private static final String CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER = "cloud_turbo_sched_enable_core_app_optimizer";
    private static final String CLOUD_TURBO_SCHED_ENABLE_CORE_TOP20_APP_OPTIMIZER = "cloud_turbo_sched_enable_core_top20_app_optimizer";
    private static final String CLOUD_TURBO_SCHED_ENABLE_V2 = "cloud_turbo_sched_enable_v2";
    private static final String CLOUD_TURBO_SCHED_FRAME_TURBO_WHITE_LIST = "cloud_turbo_sched_frame_turbo_white_list";
    private static final String CLOUD_TURBO_SCHED_LINK_APP_LIST = "cloud_turbo_sched_link_app_list";
    private static final String CLOUD_TURBO_SCHED_POLICY_LIST = "cloud_turbo_sched_policy_list";
    private static final String CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE = "cloud_turbo_sched_thermal_break_enable";
    private static final String COMMAND_DRY_RUN = "dry_run";
    private static final String ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD = "cloud_turbo_sched_thermal_break_threshold";
    public static final boolean DEBUG;
    private static List<String> DEFAULT_APP_LIST = null;
    private static List<String> DEFAULT_GL_APP_LIST = null;
    private static List<String> DEFAULT_TOP20_APP_LIST = null;
    public static final String DPS_FORCE_CLUSTER_SCHED_ENABLE_PATH = "/sys/module/metis/parameters/force_cluster_sched_enable";
    public static final String DPS_FORCE_FORBIDDEN_WALT_LB_PATH = "/sys/module/metis/parameters/force_forbidden_walt_lb";
    public static final String DPS_FORCE_VIPTASK_SELECT_RQ_PATH = "/sys/module/metis/parameters/force_viptask_select_rq";
    public static final String DPS_IP_PREFER_CLUSTER_PATH = "/sys/module/metis/parameters/ip_prefer_cluster";
    public static final String DPS_RENDER_PREFER_CLUSTER_PATH = "/sys/module/metis/parameters/render_prefer_cluster";
    public static final String DPS_STASK_PREFER_CLUSTER_PATH = "/sys/module/metis/parameters/stask_prefer_cluster";
    public static final String DPS_VIP_PREFER_FREQ_PATH = "/sys/module/metis/parameters/vip_prefer_freq";
    public static final String GAEA_PATH = "/sys/module/gaea/parameters/mi_gaea_enable";
    private static final int GET_BUFFER_TX_COUNT_TRANSACTION = 1011;
    protected static final int MAX_HISTORY_ITEMS;
    private static final int METIS_BOOST_DURATION = 40;
    private static final String METIS_BOOST_DURATION_PATH = "sys/module/metis/parameters/mi_boost_duration";
    private static final String METIS_DEV_PATH = "/dev/metis";
    private static final int METIS_IOCTL_BOOST_CMD = 6;
    public static final String METIS_VERSION_PATH = "/sys/module/metis/parameters/version";
    private static final String NO_THERMAL_BREAK_STRING = " cpu4:2419200 cpu7:2841600";
    public static final String ORI_TURBO_SCHED_PATH = "/sys/module/migt/parameters/mi_viptask";
    private static final String PROCESS_ALIEXPRESS = "com.alibaba.aliexpresshd";
    private static final String PROCESS_ALIPAY = "com.eg.android.AlipayGphone";
    private static final String PROCESS_AMAZON_SHOPPING = "com.amazon.mShop.android.shopping";
    private static final String PROCESS_ARTICLE_LITE = "com.ss.android.article.lite";
    private static final String PROCESS_ARTICLE_NEWS = "com.ss.android.article.news";
    private static final String PROCESS_ARTICLE_VIDEO = "com.ss.android.article.video";
    private static final String PROCESS_AWEME = "com.ss.android.ugc.aweme";
    private static final String PROCESS_AWEME_LITE = "com.ss.android.ugc.aweme.lite";
    private static final String PROCESS_BAIDU = "com.baidu.searchbox";
    private static final String PROCESS_BAIDU_DISK = "com.baidu.netdisk";
    private static final String PROCESS_BILIBILI = "tv.danmaku.bili";
    private static final String PROCESS_BILIBILI_HD = "tv.danmaku.bilibilihd";
    private static final String PROCESS_DISCORD = "com.discord";
    private static final String PROCESS_DOUYU = "air.tv.douyu.android";
    private static final String PROCESS_DRAGON_READ = "com.dragon.read";
    private static final String PROCESS_FACEBOOK = "com.facebook.katana";
    private static final String PROCESS_GIFMAKER = "com.smile.gifmaker";
    private static final String PROCESS_HUNANTV_IMGO = "com.hunantv.imgo.activity";
    private static final String PROCESS_HUYA = "com.duowan.kiwi";
    private static final String PROCESS_INSHOT = "com.camerasideas.instashot";
    private static final String PROCESS_INSTAGRAM = "com.instagram.android";
    private static final String PROCESS_KMXS_READER = "com.kmxs.reader";
    private static final String PROCESS_KUAISHOU_NEBULA = "com.kuaishou.nebula";
    private static final String PROCESS_KUGOU = "com.kugou.android";
    private static final String PROCESS_LAZADA = "com.lazada.android";
    private static final String PROCESS_LINKEDIN = "com.linkedin.android";
    private static final String PROCESS_MINIMAP = "com.autonavi.minimap";
    private static final String PROCESS_MOBILEQQ = "com.tencent.mobileqq";
    private static final String PROCESS_MOBILEQQ_QZONE = "com.tencent.mobileqq:qzone";
    private static final String PROCESS_MOJ = "in.mohalla.video";
    private static final String PROCESS_MX_PLAYER = "com.mxtech.videoplayer.ad";
    private static final String PROCESS_OUTLOOK = "com.microsoft.office.outlook";
    private static final String PROCESS_PINDUODUO = "com.xunmeng.pinduoduo";
    private static final String PROCESS_PINTEREST = "com.pinterest";
    private static final String PROCESS_QIYI = "com.qiyi.video";
    private static final String PROCESS_QQMUSIC = "com.tencent.qqmusic";
    private static final String PROCESS_QUARK_BROWSER = "com.quark.browser";
    private static final String PROCESS_RIMET = "com.alibaba.android.rimet";
    private static final String PROCESS_SHARECHAT = "in.mohalla.sharechat";
    private static final String PROCESS_SHAZAM = "com.shazam.android";
    private static final String PROCESS_SHEIN = "com.zzkko";
    private static final String PROCESS_SNAPCHAT = "com.snapchat.android";
    private static final String PROCESS_SPOTIFY = "com.spotify.music";
    private static final String PROCESS_TAOBAO = "com.taobao.taobao";
    private static final String PROCESS_TELEGRAM = "org.telegram.messenger";
    private static final String PROCESS_TENCENT_MTT = "com.tencent.mtt";
    private static final String PROCESS_TENCENT_NEWS = "com.tencent.news";
    private static final String PROCESS_TENCENT_QQLIVE = "com.tencent.qqlive";
    private static final String PROCESS_TRUECALLER = "com.truecaller";
    private static final String PROCESS_UCMOBILE = "com.UCMobile";
    private static final String PROCESS_WA_BUSINESS = "com.whatsapp.w4b";
    private static final String PROCESS_WECHAT = "com.tencent.mm";
    private static final String PROCESS_WEIBO = "com.sina.weibo";
    private static final String PROCESS_WHATSAPP = "com.whatsapp";
    private static final String PROCESS_WPS_OFFICE = "cn.wps.moffice_eng";
    private static final String PROCESS_X = "com.twitter.android";
    private static final String PROCESS_XHS = "com.xingin.xhs";
    private static final String PROCESS_XUEXI = "cn.xuexi.android";
    private static final String PROCESS_ZHIHU = "com.zhihu.android";
    private static String PR_NAME = null;
    public static final String RECORD_RT_PATH = "/sys/module/metis/parameters/rt_binder_client";
    public static final String SERVICE_NAME = "turbosched";
    private static List<String> TABLET_TOP20_APP_LIST = null;
    private static List<String> TABLET_TOP8_APP_LIST = null;
    public static final String TAG = "TurboSchedManagerService";
    private static Map<Integer, String> TEMPREATURE_THROSHOLD_BOOST_STRING_MAP = null;
    private static final String TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH = "/sys/module/metis/parameters/add_mi_viptask_enqueue_boost";
    private static final String TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH = "/sys/module/metis/parameters/del_mi_viptask_enqueue_boost";
    private static final String TURBO_SCHED_CANCEL_LITTLE_CORE_PATH = "/sys/module/metis/parameters/del_mi_viptask_sched_lit_core";
    private static final String TURBO_SCHED_CANCEL_PRIORITY_PATH = "/sys/module/metis/parameters/del_mi_viptask_sched_priority";
    private static final String TURBO_SCHED_LITTLE_CORE_PATH = "/sys/module/metis/parameters/add_mi_viptask_sched_lit_core";
    private static final String TURBO_SCHED_LOCK_BLOCKED_PID_PATH = "/sys/module/metis/parameters/mi_lock_blocked_pid";
    public static final String TURBO_SCHED_PATH = "/sys/module/metis/parameters/mi_viptask";
    private static final String TURBO_SCHED_PRIORITY_PATH = "/sys/module/metis/parameters/add_mi_viptask_sched_priority";
    private static final String TURBO_SCHED_THERMAL_BREAK_PATH = "/sys/module/metis/parameters/is_break_enable";
    private static final String VERSION = "v4.0.0";
    public static final String VIP_LINK_PATH = "/sys/module/metis/parameters/vip_link_enable";
    private static Object mTurboLock;
    Timer TurboSchedSDKTimer;
    TimerTask TurboSchedSDKTimerTask;
    private String enableGaeaAppProcessName;
    private String enableLinkVipAppProcessName;
    private final IActivityManager mActivityManagerService;
    private List<String> mAppsLinkVipList;
    private List<String> mAppsLockContentionList;
    private int mAppsLockContentionPid;
    private Map<Integer, Long> mBWFTidStartTimeMap;
    private List<Long> mBoostDuration;
    final ContentObserver mCloudAllowListObserver;
    final ContentObserver mCloudCoreAppOptimizerEnableObserver;
    final ContentObserver mCloudCoreTop20AppOptimizerEnableObserver;
    final ContentObserver mCloudDpsEnableObserver;
    final ContentObserver mCloudFrameTurboWhiteListObserver;
    final ContentObserver mCloudLinkWhiteListObserver;
    final ContentObserver mCloudPolicyListObserver;
    final ContentObserver mCloudSwichObserver;
    final ContentObserver mCloudThermalBreakEnableObserver;
    final ContentObserver mCloudThermalBreakThresholdObserver;
    final ContentObserver mCloudTurboschedMiuiSdkObserver;
    private Context mContext;
    private List<String> mCoreAppsPackageNameList;
    private boolean mEnableTop20;
    private FrameTurboAction mFrameTurboAction;
    private Map<String, List<String>> mFrameTurboAppMap;
    private boolean mGaeaEnable;
    private GameTurboAction mGameTurboAction;
    public HandlerThread mHandlerThread;
    private boolean mIsGlobal;
    private long mLastVsyncId;
    private boolean mLinkEnable;
    private String mMetisVersion;
    private PackageManager mPm;
    private Map<Integer, Long> mPriorityTidStartTimeMap;
    private final IProcessObserver.Stub mProcessObserver;
    private boolean mScreenOn;
    private boolean mSupportGaea;
    private TurboSchedHandler mTurboSchedHandler;
    private Map<String, TurboschedSDKParam> mTurboschedSDKParamMap;
    private final String BufferTxCountPath = "/sys/module/metis/parameters/buffer_tx_count";
    private boolean mDPSSupport = false;
    private boolean mDPSEnable = SystemProperties.getBoolean("persist.sys.turbosched.dps.enable", false);
    private boolean mThermalBreakEnabled = SystemProperties.getBoolean("persist.sys.turbosched.thermal_break.enable", false);
    private LocalLog mHistoryLog = new LocalLog(MAX_HISTORY_ITEMS);
    private boolean mTurboEnabled = SystemProperties.getBoolean("persist.sys.turbosched.enable", false);
    private boolean mTurboEnabledV2 = SystemProperties.getBoolean("persist.sys.turbosched.enable_v2", true);
    private boolean mCloudControlEnabled = false;
    private boolean mTurboNodeExist = false;
    private IBinder mSurfaceFlinger = null;
    private List<String> mCallerAllowList = new ArrayList(Arrays.asList("com.miui.home", "com.miui.personalassistant"));
    private List<String> mStartUpAllowList = new ArrayList(Arrays.asList("com.android.provision"));
    private boolean mInStartUpMode = true;
    private boolean mStartUpModeInitFinish = false;
    private boolean mKernelEnabelUpdate = false;
    private List<String> mPolicyListBackup = TurboSchedConfig.getPolicyList();
    private boolean mIsOnScroll = false;
    private String mProcessName = "";
    private long mFgCorePid = -1;
    private long mFgCoreUid = -1;
    private List<Integer> mTurboschedSdkTidsList = new ArrayList();
    private Object mTurboschedSdkTidsLock = new Object();
    private boolean mRegistedForegroundReceiver = false;
    private boolean mReceiveCloudData = SystemProperties.getBoolean("persist.sys.turbosched.receive.cloud.data", true);
    private List<String> m8550Devices = new ArrayList(Arrays.asList("nuwa", "fuxi", "socrates", "ishtar", "babylon"));
    private List<String> m8475Devices = new ArrayList(Arrays.asList("yudi", "marble", "thor", "unicorn", "mayfly", "zizhan"));
    private List<String> m8450Devices = new ArrayList(Arrays.asList("zeus", "cupid"));

    static {
        boolean z = Build.IS_DEBUGGABLE;
        DEBUG = z;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP = linkedHashMap;
        linkedHashMap.put(50, " cpu4:2112000 cpu7:2054400");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(45, " cpu4:2227200 cpu7:2169600");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(40, " cpu4:2342400 cpu7:2284800");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(35, " cpu4:2419200 cpu7:2400000");
        MAX_HISTORY_ITEMS = z ? 16384 : 4096;
        mTurboLock = new Object();
        PR_NAME = SystemProperties.get("ro.product.name", "");
        DEFAULT_APP_LIST = new ArrayList(Arrays.asList(PROCESS_GIFMAKER, PROCESS_WEIBO, PROCESS_ARTICLE_NEWS, PROCESS_TAOBAO, PROCESS_WECHAT, PROCESS_AWEME, PROCESS_BILIBILI));
        DEFAULT_TOP20_APP_LIST = new ArrayList(Arrays.asList(PROCESS_GIFMAKER, PROCESS_WEIBO, PROCESS_ARTICLE_NEWS, PROCESS_TAOBAO, PROCESS_WECHAT, PROCESS_AWEME, PROCESS_BILIBILI, PROCESS_KUAISHOU_NEBULA, PROCESS_MOBILEQQ, PROCESS_MOBILEQQ_QZONE, PROCESS_AWEME_LITE, PROCESS_TENCENT_QQLIVE, PROCESS_ARTICLE_LITE, PROCESS_BAIDU, PROCESS_PINDUODUO, PROCESS_UCMOBILE, PROCESS_DRAGON_READ, PROCESS_QIYI, PROCESS_ARTICLE_VIDEO, "com.eg.android.AlipayGphone", PROCESS_TENCENT_MTT, PROCESS_KMXS_READER));
        TABLET_TOP8_APP_LIST = new ArrayList(Arrays.asList(PROCESS_AWEME, PROCESS_WECHAT, PROCESS_RIMET, PROCESS_GIFMAKER, PROCESS_KUAISHOU_NEBULA, PROCESS_BILIBILI_HD, PROCESS_AWEME_LITE, PROCESS_MOBILEQQ));
        TABLET_TOP20_APP_LIST = new ArrayList(Arrays.asList(PROCESS_AWEME, PROCESS_WECHAT, PROCESS_RIMET, PROCESS_GIFMAKER, PROCESS_KUAISHOU_NEBULA, PROCESS_BILIBILI_HD, PROCESS_AWEME_LITE, PROCESS_MOBILEQQ, PROCESS_BAIDU_DISK, PROCESS_BILIBILI, PROCESS_ARTICLE_NEWS, PROCESS_BAIDU, PROCESS_XHS, PROCESS_DRAGON_READ, PROCESS_TENCENT_QQLIVE, PROCESS_ARTICLE_VIDEO, PROCESS_ARTICLE_LITE, PROCESS_HUYA, PROCESS_QUARK_BROWSER, PROCESS_DOUYU));
        DEFAULT_GL_APP_LIST = new ArrayList(Arrays.asList(PROCESS_WHATSAPP, PROCESS_FACEBOOK, PROCESS_INSTAGRAM, PROCESS_TELEGRAM, PROCESS_SNAPCHAT, PROCESS_WPS_OFFICE, PROCESS_TRUECALLER, PROCESS_SPOTIFY, PROCESS_MX_PLAYER, PROCESS_X, PROCESS_PINTEREST, PROCESS_SHARECHAT, PROCESS_WA_BUSINESS, PROCESS_AMAZON_SHOPPING, PROCESS_LINKEDIN, PROCESS_MOJ, PROCESS_ALIEXPRESS, PROCESS_INSHOT, PROCESS_OUTLOOK, PROCESS_SHAZAM, PROCESS_LAZADA, PROCESS_SHEIN, PROCESS_DISCORD));
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final TurboSchedManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new TurboSchedManagerService(context);
        }

        public void onStart() {
            publishBinderService(TurboSchedManagerService.SERVICE_NAME, this.mService);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TurboschedSDKParam {
        private long mSetTime;
        private long mStartTime;
        private int mTid;
        private String mType;

        public TurboschedSDKParam(long startTime, long setTime, int tid, String type) {
            this.mStartTime = startTime;
            this.mSetTime = setTime;
            this.mTid = tid;
            this.mType = type;
        }
    }

    public TurboSchedManagerService(Context context) {
        this.mHandlerThread = null;
        this.mTurboSchedHandler = null;
        this.mPm = null;
        this.mIsGlobal = (!miui.os.Build.IS_INTERNATIONAL_BUILD && "CN".equals(SystemProperties.get("ro.miui.region", "unknown")) && (!PR_NAME.contains(d.h) || PR_NAME.contains("_cn") || PR_NAME.contains("_CN"))) ? false : true;
        this.mLinkEnable = SystemProperties.getBoolean("persist.sys.turbosched.link.enable", true);
        this.mSupportGaea = SystemProperties.getBoolean("persist.sys.turbosched.support.gaea", false);
        this.mGaeaEnable = SystemProperties.getBoolean("persist.sys.turbosched.gaea.enable", false);
        this.mEnableTop20 = SystemProperties.getBoolean("persist.sys.turbosched.enabletop20app", false);
        this.mCoreAppsPackageNameList = DEFAULT_APP_LIST;
        this.mAppsLinkVipList = new ArrayList(Arrays.asList(PROCESS_GIFMAKER, PROCESS_KUAISHOU_NEBULA, PROCESS_AWEME, PROCESS_MOBILEQQ, PROCESS_ARTICLE_NEWS, PROCESS_ARTICLE_LITE, PROCESS_TENCENT_NEWS, PROCESS_RIMET, PROCESS_XUEXI, PROCESS_ZHIHU, PROCESS_KUGOU, PROCESS_HUNANTV_IMGO, PROCESS_QQMUSIC, PROCESS_BILIBILI, PROCESS_TENCENT_QQLIVE, PROCESS_UCMOBILE, PROCESS_WEIBO, PROCESS_MOBILEQQ_QZONE, PROCESS_MINIMAP));
        this.mAppsLockContentionPid = -1;
        this.mAppsLockContentionList = new ArrayList(Arrays.asList("com.miui.home"));
        this.mScreenOn = true;
        this.mBWFTidStartTimeMap = new HashMap();
        this.mPriorityTidStartTimeMap = new HashMap();
        this.enableGaeaAppProcessName = "";
        this.enableLinkVipAppProcessName = "";
        this.TurboSchedSDKTimer = null;
        this.TurboSchedSDKTimerTask = null;
        this.mTurboschedSDKParamMap = new HashMap();
        this.mCloudSwichObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_ALL))) {
                    Slog.i(TurboSchedManagerService.TAG, "mCloudSwichObserver onChange!");
                    TurboSchedManagerService.this.updateEnableProp(false);
                }
                Slog.i(TurboSchedManagerService.TAG, "mCloudSwichObserver onChange done!");
            }
        };
        this.mCloudTurboschedMiuiSdkObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_V2))) {
                    Slog.i(TurboSchedManagerService.TAG, "mCloudTurboschedMiuiSdkObserver onChange!");
                    TurboSchedManagerService.this.updateEnableMiuiSdkProp(false);
                }
                Slog.i(TurboSchedManagerService.TAG, "mCloudTurboschedMiuiSdkObserver onChange done!");
            }
        };
        this.mCloudCoreAppOptimizerEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER))) {
                    Slog.i(TurboSchedManagerService.TAG, "mCloudCoreAppOptimizerEnableObserver onChange!");
                    TurboSchedManagerService.this.updateCoreAppOptimizerEnableProp();
                }
                Slog.i(TurboSchedManagerService.TAG, "mCloudCoreAppOptimizerEnableObserver onChange done!");
            }
        };
        this.mCloudCoreTop20AppOptimizerEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_CORE_TOP20_APP_OPTIMIZER))) {
                    TurboSchedManagerService.this.updateCoreTop20AppOptimizerEnableProp();
                }
            }
        };
        this.mCloudAllowListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ALLOW_LIST))) {
                    TurboSchedManagerService.this.updateCloudAllowListProp();
                }
            }
        };
        this.mCloudPolicyListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_POLICY_LIST))) {
                    Slog.i(TurboSchedManagerService.TAG, "Cloud PolicyList Observer onChange!");
                    TurboSchedManagerService.this.updateCloudPolicyListProp();
                }
                Slog.i(TurboSchedManagerService.TAG, "Cloud PolicyList Observer onChange done!");
            }
        };
        this.mCloudThermalBreakEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE))) {
                    TurboSchedManagerService.this.updateCloudThermalBreakEnableProp();
                }
            }
        };
        this.mCloudThermalBreakThresholdObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD))) {
                    TurboSchedManagerService.this.updateCloudThermalBreakThresholdProp();
                }
            }
        };
        this.mCloudLinkWhiteListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_LINK_APP_LIST))) {
                    TurboSchedManagerService.this.updateCloudLinkWhiteListProp();
                }
            }
        };
        this.mCloudFrameTurboWhiteListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.10
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_FRAME_TURBO_WHITE_LIST))) {
                    TurboSchedManagerService.this.updateCloudFrameTurboWhiteListProp();
                }
            }
        };
        this.mCloudDpsEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.11
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_DPS_ENABLE))) {
                    TurboSchedManagerService.this.updateCloudDpsEnableProp();
                }
            }
        };
        this.mProcessObserver = new IProcessObserver.Stub() { // from class: com.miui.server.turbosched.TurboSchedManagerService.12
            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
                String curProcessName = TurboSchedManagerService.this.getProcessNameByPid(pid);
                if (!TurboSchedManagerService.this.mStartUpModeInitFinish && foregroundActivities && "com.miui.home".equals(curProcessName)) {
                    TurboSchedManagerService.this.exitStartUpMode();
                }
                boolean is_enable = TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled();
                if (!is_enable) {
                    return;
                }
                if (TurboSchedConfig.getPolicyList().contains("frame_turbo")) {
                    TurboSchedManagerService.this.mFrameTurboAction.onForegroundActivitiesChanged(curProcessName, foregroundActivities);
                }
                if (foregroundActivities && TurboSchedManagerService.this.mScreenOn) {
                    TurboSchedManagerService.this.mProcessName = curProcessName;
                    if (TurboSchedManagerService.this.mProcessName != null && TurboSchedMonitor.getInstance().isCoreApp(TurboSchedManagerService.this.mProcessName)) {
                        TurboSchedManagerService.this.mFgCorePid = pid;
                        TurboSchedManagerService.this.mFgCoreUid = uid;
                    }
                }
                if (curProcessName != null && TurboSchedManagerService.this.mAppsLockContentionList.contains(curProcessName) && is_enable && foregroundActivities && TurboSchedManagerService.this.mScreenOn && pid > 0 && pid != TurboSchedManagerService.this.mAppsLockContentionPid) {
                    boolean isSuccess = TurboSchedManagerService.this.writeTurboSchedNode("LINK", TurboSchedManagerService.TURBO_SCHED_LOCK_BLOCKED_PID_PATH, String.valueOf(pid));
                    if (isSuccess) {
                        TurboSchedManagerService.this.mAppsLockContentionPid = pid;
                    }
                    if (TurboSchedMonitor.getInstance().isDebugMode()) {
                        Slog.i(TurboSchedManagerService.TAG, "Lock Contention, isSuccess:" + isSuccess + " Blocked Task Pid " + pid);
                    }
                }
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
            }

            public void onProcessDied(int pid, int uid) {
            }
        };
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(SERVICE_NAME);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mTurboSchedHandler = new TurboSchedHandler(this.mHandlerThread.getLooper());
        TurboSchedSceneManager.getInstance().initialize(context, this.mHandlerThread.getLooper());
        this.mPm = this.mContext.getPackageManager();
        this.mActivityManagerService = ActivityManager.getService();
        onServiceStart();
        registeForegroundReceiver();
        if (this.mReceiveCloudData) {
            registerObserver();
            updateCloudControlProp();
        } else {
            updateLocalProp();
        }
        new TurboSchedBroadcastReceiver();
        String str = String.valueOf(40);
        writeTurboSchedNode("B-Duration", METIS_BOOST_DURATION_PATH, str);
        initTopAppList();
        FrameTurboAction frameTurboAction = new FrameTurboAction();
        this.mFrameTurboAction = frameTurboAction;
        this.mFrameTurboAppMap = frameTurboAction.mFrameTurboAppMap;
        this.mGameTurboAction = new GameTurboAction();
        initStartUpMode();
    }

    private void onServiceStart() {
        this.mTurboNodeExist = TurboSchedUtil.checkFileAccess(TURBO_SCHED_PATH);
        this.mBoostDuration = TurboSchedMonitor.getInstance().getBoostDuration();
        String readValueFromFile = TurboSchedUtil.readValueFromFile(METIS_VERSION_PATH);
        this.mMetisVersion = readValueFromFile;
        if (readValueFromFile != null) {
            this.mDPSSupport = true;
        }
    }

    private void initStartUpMode() {
        this.mTurboEnabledV2 = true;
        enableFrameBoostInKernel(true);
        if (!this.mRegistedForegroundReceiver) {
            registeForegroundReceiver();
        }
        TurboSchedConfig.setPolicyList(TurboSchedConfig.POLICY_ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exitStartUpMode() {
        this.mStartUpModeInitFinish = true;
        this.mInStartUpMode = false;
        if (!this.mKernelEnabelUpdate) {
            enableFrameBoostInKernel(false);
        }
        TurboSchedConfig.setPolicyList(this.mPolicyListBackup);
    }

    private boolean isTurboEnabled() {
        if (this.mTurboEnabled) {
            return true;
        }
        return this.mTurboEnabledV2;
    }

    private boolean setTurboSchedActionInternal(int[] tids, long time, String path) {
        boolean isSuccess;
        if (!isTurboEnabled()) {
            this.mHistoryLog.log("TS [V]: not enable");
            return false;
        }
        if (tids.length > 3) {
            this.mHistoryLog.log("TS [V]: tids length over limit");
            return false;
        }
        sendTraceBeginMsg("V");
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Trace.traceBegin(64L, "setTurboSchedActionInternal");
        }
        synchronized (mTurboLock) {
            StringBuilder sb = new StringBuilder();
            sb.append(tids[0]);
            for (int i = 1; i < tids.length && tids[i] != 0; i++) {
                sb.append(":");
                sb.append(tids[i]);
            }
            sb.append("-");
            sb.append(time);
            String str = sb.toString();
            isSuccess = writeTurboSchedNode("V", path, str);
            if (!isSuccess && TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.d(TAG, "setTurboSchedActionInternal, not success, check file:" + path);
            }
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Trace.traceEnd(64L);
        }
        sendTraceEndMsg();
        this.mHistoryLog.log("TS [V]: success: " + isSuccess);
        return isSuccess;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean writeTurboSchedNode(String logTag, String path, String value) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        try {
            PrintWriter writer = new PrintWriter(path);
            try {
                writer.write(value);
                this.mHistoryLog.log("[ " + logTag + " ] write message : " + value);
                writer.close();
                return true;
            } finally {
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write path : " + path + "  value : " + value, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TurboSchedHandler extends Handler {
        static final int MSG_TRACE_BEGIN = 0;
        static final int MSG_TRACE_END = 1;

        private TurboSchedHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 0:
                    TraceInfo info = (TraceInfo) msg.obj;
                    traceBegin(info.name, info.desc, info.startTime);
                    int tid = msg.arg1;
                    Trace.traceBegin(64L, "P-Tid:" + tid);
                    return;
                case 1:
                    Trace.traceEnd(64L);
                    traceEnd();
                    return;
                default:
                    return;
            }
        }

        private void traceBegin(String name, String desc, long stratTime) {
            long delay = System.currentTimeMillis() - stratTime;
            if (desc != null && !desc.isEmpty()) {
                desc = "(" + desc + ")";
            }
            String tag = name + desc + " delay=" + delay;
            Trace.traceBegin(64L, tag);
        }

        private void traceEnd() {
            Trace.traceEnd(64L);
        }
    }

    private void sendTraceBeginMsg(String name) {
        sendTraceBeginMsg(name, "");
    }

    private void sendTraceBeginMsg(String name, String desc) {
        if (this.mTurboSchedHandler == null) {
            Slog.d(TAG, "handler is null while sending trace begin message");
            return;
        }
        TraceInfo info = new TraceInfo();
        info.name = name;
        info.desc = desc;
        info.startTime = System.currentTimeMillis();
        Message msg = this.mTurboSchedHandler.obtainMessage(0, info);
        int tid = Process.myTid();
        msg.arg1 = tid;
        this.mTurboSchedHandler.sendMessage(msg);
    }

    private void sendTraceEndMsg() {
        TurboSchedHandler turboSchedHandler = this.mTurboSchedHandler;
        if (turboSchedHandler == null) {
            Slog.d(TAG, "handler is null while sending trace end message");
        } else {
            Message msg = turboSchedHandler.obtainMessage(1);
            this.mTurboSchedHandler.sendMessage(msg);
        }
    }

    private void registerObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_ALL), false, this.mCloudSwichObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_V2), false, this.mCloudTurboschedMiuiSdkObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER), false, this.mCloudCoreAppOptimizerEnableObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_CORE_TOP20_APP_OPTIMIZER), false, this.mCloudCoreTop20AppOptimizerEnableObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ALLOW_LIST), false, this.mCloudAllowListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_POLICY_LIST), false, this.mCloudPolicyListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE), false, this.mCloudThermalBreakEnableObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_LINK_APP_LIST), false, this.mCloudLinkWhiteListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_FRAME_TURBO_WHITE_LIST), false, this.mCloudFrameTurboWhiteListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_DPS_ENABLE), false, this.mCloudDpsEnableObserver, -2);
    }

    private void updateCloudControlProp() {
        updateCoreAppOptimizerEnableProp();
        updateEnableProp(TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled());
        updateCoreTop20AppOptimizerEnableProp();
        updateCloudAllowListProp();
        updateCloudPolicyListProp();
        updateCloudThermalBreakEnableProp();
        updateCloudThermalBreakThresholdProp();
        updateCloudLinkWhiteListProp();
        updateCloudFrameTurboWhiteListProp();
        updateCloudDpsEnableProp();
    }

    private void updateLocalProp() {
        enableFrameBoostInKernel(TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled());
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled()) {
            this.mKernelEnabelUpdate = true;
        }
        setThermalBreakEnable(this.mThermalBreakEnabled);
        updateLocalPolicyListProp();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEnableProp(boolean z) {
        boolean z2 = false;
        String stringForUser = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_ALL, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        if (stringForUser != null && !stringForUser.isEmpty()) {
            z2 = Boolean.parseBoolean(stringForUser);
            this.mCloudControlEnabled = z2;
            Slog.d(TAG, "cloud turbosched v1 set received: " + this.mCloudControlEnabled);
        }
        enableTurboSched(z2);
        Slog.d(TAG, "cloud data update done - turbosched v1: " + z2);
        updateEnableMiuiSdkProp(z2);
        if (!z) {
            TurboSchedMonitor.getInstance().enableCoreAppOptimizer(z2 ? 1 : 0);
            enableFrameBoostInKernel(z2);
            if (z2) {
                this.mKernelEnabelUpdate = true;
            }
        }
        if (z2 && !this.mRegistedForegroundReceiver) {
            registeForegroundReceiver();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEnableMiuiSdkProp(boolean optimizerInitEnabled) {
        boolean optimizerEnabled = false;
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_V2, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        if (enableStr != null && !enableStr.isEmpty()) {
            optimizerEnabled = Boolean.parseBoolean(enableStr);
            this.mCloudControlEnabled = optimizerEnabled;
            Slog.d(TAG, "cloud turbosched v2 set received: " + this.mCloudControlEnabled);
            if (!this.mRegistedForegroundReceiver) {
                registeForegroundReceiver();
            }
        }
        boolean optimizerEnabled2 = optimizerEnabled | optimizerInitEnabled;
        enableTurboSchedV2(optimizerEnabled2);
        Slog.d(TAG, "cloud data update done - turbosched v2: " + optimizerEnabled2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCoreAppOptimizerEnableProp() {
        boolean z = false;
        String stringForUser = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        if (stringForUser != null && !stringForUser.isEmpty()) {
            z = Boolean.parseBoolean(stringForUser);
            Slog.d(TAG, "cloud core app optimizer set received: " + z);
        }
        TurboSchedMonitor.getInstance().enableCoreAppOptimizer(z ? 1 : 0);
        enableGaea(z);
        if (z && !this.mRegistedForegroundReceiver) {
            registeForegroundReceiver();
        }
        enableFrameBoostInKernel(z);
        if (z) {
            this.mKernelEnabelUpdate = true;
        }
        Slog.d(TAG, "cloud data update done - coreapp enable: " + z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCoreTop20AppOptimizerEnableProp() {
        boolean optimizerEnabled = false;
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_CORE_TOP20_APP_OPTIMIZER, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        if (enableStr != null && !enableStr.isEmpty()) {
            optimizerEnabled = Boolean.parseBoolean(enableStr);
            Slog.d(TAG, "cloud core top20 app optimizer set received: " + optimizerEnabled);
        }
        syncTop20AppList(optimizerEnabled);
        Slog.d(TAG, "cloud data update done - core top20 app enable: " + optimizerEnabled);
    }

    private void registeForegroundReceiver() {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() || this.mInStartUpMode) {
            try {
                Slog.i(TAG, "registerProcessObserver!");
                this.mActivityManagerService.registerProcessObserver(this.mProcessObserver);
                this.mRegistedForegroundReceiver = true;
            } catch (RemoteException e) {
                Slog.e(TAG, "registerProcessObserver failed");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudAllowListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ALLOW_LIST, -2);
        if (this.mReceiveCloudData && str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            this.mCallerAllowList.clear();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    this.mCallerAllowList.add(name);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudPolicyListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_POLICY_LIST, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        List<String> policyList = new ArrayList<>();
        if (str != null && !str.isEmpty()) {
            policyList.clear();
            String[] policyListName = str.split(",");
            if (policyListName.length > 0) {
                for (String name : policyListName) {
                    policyList.add(name);
                }
                TurboSchedConfig.setPolicyList(policyList);
                this.mPolicyListBackup = policyList;
                Slog.d(TAG, "Update Cloud PolicyList Prop - mPolicyList: " + Arrays.toString(policyList.toArray()));
                return;
            }
            return;
        }
        policyList.clear();
        TurboSchedConfig.setPolicyList(policyList);
        this.mPolicyListBackup = policyList;
        Slog.d(TAG, "Update Cloud PolicyList Prop - mPolicyList: null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudDpsEnableProp() {
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_DPS_ENABLE, -2);
        if (this.mReceiveCloudData && enableStr != null && !enableStr.isEmpty()) {
            setDpsEnable(Boolean.parseBoolean(enableStr));
        }
    }

    private void setDpsEnable(boolean enable) {
        if (!this.mDPSSupport) {
            Slog.d(TAG, "DPS not support");
        } else {
            this.mDPSEnable = enable;
            SystemProperties.set("persist.sys.turbosched.dps.enable", Boolean.toString(enable));
        }
    }

    private void updateLocalPolicyListProp() {
        String str = SystemProperties.get("persist.sys.turbosched.policy_list", "");
        if (str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            List<String> policyList = new ArrayList<>();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    policyList.add(name);
                }
                TurboSchedConfig.setPolicyList(policyList);
                this.mPolicyListBackup = policyList;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudThermalBreakEnableProp() {
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE, -2);
        if (this.mReceiveCloudData && enableStr != null && !enableStr.isEmpty()) {
            setThermalBreakEnable(Boolean.parseBoolean(enableStr));
        }
    }

    private void setThermalBreakEnable(boolean enable) {
        Slog.d(TAG, "cloud thermal break set received :" + enable);
        this.mThermalBreakEnabled = enable;
        SystemProperties.set("persist.sys.turbosched.thermal_break.enable", Boolean.toString(enable));
        String enableStr = this.mThermalBreakEnabled ? "1" : "0";
        boolean isSuccess = writeTurboSchedNode("TB-Enabled", TURBO_SCHED_THERMAL_BREAK_PATH, enableStr);
        if (!isSuccess) {
            Slog.e(TAG, "write turbo sched thermal break failed");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudThermalBreakThresholdProp() {
        String thresholdStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD, -2);
        if (!this.mReceiveCloudData) {
            return;
        }
        setThermalBreakThresholdMap(thresholdStr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudLinkWhiteListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_LINK_APP_LIST, -2);
        if (this.mReceiveCloudData && str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            this.mAppsLinkVipList.clear();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    this.mAppsLinkVipList.add(name);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudFrameTurboWhiteListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_FRAME_TURBO_WHITE_LIST, -2);
        if (this.mReceiveCloudData && str != null && !str.isEmpty()) {
            String[] pkgScenes = str.split(",");
            this.mFrameTurboAction.updateWhiteList(pkgScenes);
        }
    }

    private void setThermalBreakThresholdMap(String thresholdStr) {
        if (thresholdStr != null && !thresholdStr.isEmpty()) {
            Slog.d(TAG, "thermal break threshold set received :" + thresholdStr);
            String[] thresholdList = thresholdStr.split(",");
            for (String t : thresholdList) {
                String[] thresholdValues = t.split("-");
                if (thresholdValues.length == 2) {
                    int temp = Integer.parseInt(thresholdValues[0]);
                    String threshold = " " + thresholdValues[1].trim();
                    TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(Integer.valueOf(temp), threshold.replace(d.h, " "));
                }
            }
        }
    }

    private List<Integer> setTurboSchedActionParam(String logTag, int[] tids, long time, String type) {
        int i;
        TurboSchedManagerService turboSchedManagerService = this;
        int[] iArr = tids;
        List<Integer> tidsList = new ArrayList<>();
        synchronized (turboSchedManagerService.mTurboschedSdkTidsLock) {
            try {
                try {
                    int length = iArr.length;
                    int i2 = 0;
                    while (i2 < length) {
                        int tid = iArr[i2];
                        String paramId = Integer.toString(tid) + type;
                        if (turboSchedManagerService.mTurboschedSDKParamMap.get(paramId) != null) {
                            try {
                                i = length;
                                turboSchedManagerService.mTurboschedSDKParamMap.put(paramId, new TurboschedSDKParam(System.currentTimeMillis(), time, tid, type));
                                try {
                                    turboSchedManagerService.mHistoryLog.log("TS [" + logTag + "] : tid duplication, tids: " + tid + ",time: " + time);
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } else {
                            i = length;
                            tidsList.add(Integer.valueOf(tid));
                            turboSchedManagerService.mTurboschedSDKParamMap.put(paramId, new TurboschedSDKParam(System.currentTimeMillis(), time, tid, type));
                        }
                        i2++;
                        turboSchedManagerService = this;
                        iArr = tids;
                        length = i;
                    }
                    return tidsList;
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public boolean isApiEnable() {
        return isTurboEnabled();
    }

    public String getApiVersion() {
        return VERSION;
    }

    public List<String> getPolicyList() {
        return TurboSchedConfig.getPolicyList();
    }

    public boolean setTurboSchedAction(int[] tid, long time) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        if (!checkCallerPermmsion(pid, uid)) {
            return false;
        }
        if (this.mTurboNodeExist) {
            boolean ret = setTurboSchedActionInternal(tid, time, TURBO_SCHED_PATH);
            return ret;
        }
        boolean ret2 = setTurboSchedActionInternal(tid, time, ORI_TURBO_SCHED_PATH);
        return ret2;
    }

    public void setTurboSchedActionWithoutBlock(int[] tids, long time) {
        setTurboSchedAction(tids, time);
    }

    public void setTurboSchedActionWithBoostFrequency(int[] tids, long time) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("bwf")) {
            this.mHistoryLog.log("TS [BWF] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        if (tids.length > 1) {
            this.mHistoryLog.log("TS [BWF] : failed, R: tids length must be 1, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        sendTraceBeginMsg("bwf");
        List<Integer> tidsList = setTurboSchedActionParam("BWF", tids, time, "bwf");
        if (tidsList == null || tidsList.isEmpty()) {
            return;
        }
        int[] newTids = tidsList.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray();
        boolean success = setTurboSchedAction(newTids, time);
        if (success) {
            boolean success2 = writeTidsToPath(TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH, newTids);
            if (success2) {
                setPolicyTimeoutChecker("-BWF", newTids, time, TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH, this.mBWFTidStartTimeMap);
                this.mHistoryLog.log("TS [BWF] : success, tids: " + Arrays.toString(newTids) + ",time: " + time);
            } else {
                this.mHistoryLog.log("TS [BWF] : failed, R: write failed 1, tids: " + Arrays.toString(newTids) + ",time: " + time);
            }
        } else {
            this.mHistoryLog.log("TS [BWF] : failed, R: write failed 2, tids: " + Arrays.toString(newTids) + ",time: " + time);
        }
        sendTraceEndMsg();
    }

    public int frameTurboMarkScene(String sceneId, boolean start) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("frame_turbo")) {
            this.mHistoryLog.log("TS [MARK] : failed, R: not enable, sceneId: " + sceneId + ",start: " + start);
            return -1;
        }
        int uid = Binder.getCallingUid();
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        if (!isForeground(packageName)) {
            this.mHistoryLog.log("TS [MARK] : failed, R: not foreground, sceneId: " + sceneId + ",start: " + start);
            return -6;
        }
        return this.mFrameTurboAction.markScene(packageName, sceneId, start);
    }

    public int frameTurboTrigger(boolean turbo, List<String> uiThreads, List<String> scenes) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("frame_turbo")) {
            this.mHistoryLog.log("TS [FRAME] : failed, R: not enable");
            return -1;
        }
        int pid = Binder.getCallingPid();
        if (uiThreads == null || uiThreads.size() == 0) {
            uiThreads = new ArrayList();
            uiThreads.add(String.valueOf(pid));
        }
        int uid = Binder.getCallingUid();
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        if (!isForeground(packageName)) {
            this.mHistoryLog.log("TS [FRAME] : failed, R: not foreground");
            return -6;
        }
        return this.mFrameTurboAction.triggerFrameTurbo(packageName, turbo, uiThreads, scenes);
    }

    public void frameTurboRegisterSceneCallback(ITurboSchedManager.IFrameTurboSceneCallback callback) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("frame_turbo")) {
            this.mHistoryLog.log("TS [FRAME] : failed, R: not enable");
        } else {
            this.mFrameTurboAction.registerSceneCallback(callback);
        }
    }

    public void frameTurboUnregisterSceneCallback(ITurboSchedManager.IFrameTurboSceneCallback callback) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("frame_turbo")) {
            this.mHistoryLog.log("TS [FRAME] : failed, R: not enable");
        } else {
            this.mFrameTurboAction.unregisterSceneCallback(callback);
        }
    }

    public void setTurboSchedActionWithPriority(int[] tids, long time) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("priority")) {
            this.mHistoryLog.log("TS [PRIORITY] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        sendTraceBeginMsg("priority");
        List<Integer> tidsList = setTurboSchedActionParam("PRIORITY", tids, time, "priority");
        if (tidsList == null || tidsList.isEmpty()) {
            return;
        }
        int[] newTids = tidsList.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray();
        boolean success = writeTidsToPath(TURBO_SCHED_PRIORITY_PATH, newTids);
        if (success) {
            setPolicyTimeoutChecker("-PRIORITY", newTids, time, TURBO_SCHED_CANCEL_PRIORITY_PATH, this.mPriorityTidStartTimeMap);
            this.mHistoryLog.log("TS [PRIORITY] : success, tids: " + Arrays.toString(newTids) + ",time: " + time);
        } else {
            this.mHistoryLog.log("TS [PRIORITY] : failed, R: write failed, tids: " + Arrays.toString(newTids) + ",time: " + time);
        }
        sendTraceEndMsg();
    }

    public void setTurboSchedActionToLittleCore(int[] tids, long time) {
        if (!isTurboEnabled() || !TurboSchedConfig.getPolicyList().contains("lc")) {
            this.mHistoryLog.log("TS [LC] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        sendTraceBeginMsg("lc");
        List<Integer> tidsList = setTurboSchedActionParam("LC", tids, time, "lc");
        if (tidsList == null || tidsList.isEmpty()) {
            return;
        }
        int[] newTids = tidsList.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray();
        boolean success = writeTidsToPath(TURBO_SCHED_LITTLE_CORE_PATH, newTids);
        if (success) {
            setPolicyTimeoutChecker("-LC", newTids, time, TURBO_SCHED_CANCEL_LITTLE_CORE_PATH, this.mPriorityTidStartTimeMap);
            this.mHistoryLog.log("TS [LC] : success, tids: " + Arrays.toString(newTids) + ",time: " + time);
        } else {
            this.mHistoryLog.log("TS [LC] : failed, R: write failed, newTids: " + Arrays.toString(newTids) + ",time: " + time);
        }
        sendTraceEndMsg();
    }

    public void registerStateChangeCallback(String pkgName, ITurboSchedManager.ITurboSchedStateChangeCallback cb) {
        TurboSchedSceneManager.getInstance().registerStateChangeCallback(pkgName, cb);
    }

    public void unregisterStateChangeCallback(String pkgName, ITurboSchedManager.ITurboSchedStateChangeCallback cb) {
        TurboSchedSceneManager.getInstance().unregisterStateChangeCallback(pkgName, cb);
    }

    public void onFocusedWindowChangeLocked(String focus, int type) {
        TurboSchedSceneManager.getInstance().onFocusedWindowChangeLocked(focus, type);
    }

    private boolean isFgDrawingFrame(int[] tids) {
        for (int i : tids) {
            if (i == this.mFgCorePid) {
                return true;
            }
        }
        return false;
    }

    public void notifyOnScroll(boolean isOnScroll) {
        this.mIsOnScroll = isOnScroll;
    }

    public void setTurboSchedActionWithId(int[] tids, long time, long id, int mode) {
        if (isTurboEnabled() && isFgDrawingFrame(tids) && this.mIsOnScroll && TurboSchedMonitor.getInstance().isCoreApp(this.mProcessName) && id != this.mLastVsyncId) {
            int bufferTx = getBufferTxCount();
            if (bufferTx >= 1) {
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "bufferTx larger than 2, dont't need boost, FluencyOptimizer");
                }
            } else {
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "setTurboSchedActionWithId, id:" + id + ", bufferTx:" + bufferTx + ", FluencyOptimizer");
                }
                this.mLastVsyncId = id;
                breakThermlimit(1, time);
                metisFrameBoost(this.mBoostDuration.get(0).intValue());
            }
        }
    }

    public int checkBoostPermission() {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String str = getProcessNameByPid(pid);
        int ret = -1;
        if (this.mFgCoreUid != -1) {
            if (str != null && TurboSchedMonitor.getInstance().isCoreApp(str) && uid == this.mFgCoreUid) {
                ret = 1;
            } else {
                ret = 0;
            }
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "checkBoostPermission, uid:" + uid + ", pid:" + pid + ", str:" + str + ", mFgCoreUid:" + this.mFgCoreUid + ", ret:" + ret);
        }
        return ret;
    }

    public boolean checkPackagePermission(String packageName) {
        if (packageName != null && TurboSchedMonitor.getInstance().isCoreApp(packageName)) {
            return true;
        }
        return false;
    }

    public boolean isCoreApp() {
        int pid = Binder.getCallingPid();
        String processName = getProcessNameByPid(pid);
        return TurboSchedMonitor.getInstance().isCoreApp(processName);
    }

    public List<String> getCoreAppList() {
        return this.mCoreAppsPackageNameList;
    }

    public List<String> getCallerAllowList() {
        return this.mCallerAllowList;
    }

    public void triggerBoostAction(int boostMs) {
        metisFrameBoost(boostMs);
    }

    public void triggerBoostTask(int tid, int boostSec) {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() && TurboSchedUtil.checkFileAccess(METIS_DEV_PATH)) {
            synchronized (mTurboLock) {
                int handle = NativeTurboSchedManager.nativeOpenDevice(METIS_DEV_PATH);
                if (handle >= 0) {
                    NativeTurboSchedManager.nativeTaskBoost(handle, tid, boostSec);
                    NativeTurboSchedManager.nativeCloseDevice(handle);
                }
            }
        }
    }

    public void breakThermlimit(int boost, long time) {
        String str;
        if (!TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() || !this.mThermalBreakEnabled) {
            return;
        }
        sendTraceBeginMsg("breakThermlimit", String.valueOf(boost));
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, begin");
        }
        if (boost == 0) {
            str = "boost:0";
        } else {
            String strThermalBreak = getThermalBreakString();
            str = "boost:1" + strThermalBreak + " time:" + time;
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, str:" + str);
        }
        boolean isSuccess = writeTurboSchedNode("BT-Limit", BOOST_THERMAL_PATH, str);
        sendTraceEndMsg();
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, isSuccess:" + isSuccess + ", FluencyOptimizer");
        }
    }

    public void setSceneAction(String scene, String action, int timeout) {
        char c;
        switch (action.hashCode()) {
            case 1742114739:
                if (action.equals("TS_ACTION_FLING")) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            default:
                return;
        }
    }

    private int getBufferTxCount() {
        Parcel data = null;
        Parcel reply = null;
        try {
            try {
                if (this.mSurfaceFlinger == null) {
                    this.mSurfaceFlinger = ServiceManager.getService(DumpSysInfoUtil.SURFACEFLINGER);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to get Buffer-Tx count");
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return -1;
                }
            }
            if (this.mSurfaceFlinger == null) {
                if (0 != 0) {
                    data.recycle();
                }
                if (0 == 0) {
                    return -1;
                }
                reply.recycle();
                return -1;
            }
            reply = Parcel.obtain();
            data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            this.mSurfaceFlinger.transact(GET_BUFFER_TX_COUNT_TRANSACTION, data, reply, 0);
            int readInt = reply.readInt();
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            return readInt;
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    private boolean isStartUpModeAllowed(String packageName, int uid) {
        if (this.mInStartUpMode) {
            return (packageName != null && this.mStartUpAllowList.contains(packageName)) || uid == 0 || uid == 1000;
        }
        return false;
    }

    private boolean checkCallerPermmsion(int pid, int uid) {
        String str = ProcessUtils.getPackageNameByPid(pid);
        if (uid == 0) {
            return true;
        }
        if ((str != null && this.mCallerAllowList.contains(str)) || isStartUpModeAllowed(str, uid)) {
            return true;
        }
        this.mHistoryLog.log("TS [V] : caller is not allow: uid: " + uid + ", pkg: " + str + ", pid: " + pid);
        return false;
    }

    private String getPackageNameFromUid(int uid) {
        String packName = null;
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packName = packageManager.getNameForUid(uid);
        }
        if (packName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getProcessNameByPid(int pid) {
        String processName = ProcessUtils.getProcessNameByPid(pid);
        return processName;
    }

    public boolean isFileAccess() {
        File file = new File(TURBO_SCHED_PATH);
        return file.exists();
    }

    private void metisFrameBoost(int boostMs) {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() && TurboSchedUtil.checkFileAccess(METIS_DEV_PATH)) {
            synchronized (mTurboLock) {
                int handle = NativeTurboSchedManager.nativeOpenDevice(METIS_DEV_PATH);
                if (handle >= 0) {
                    if (TurboSchedMonitor.getInstance().isDebugMode()) {
                        Trace.traceBegin(64L, "metisFrameBoost");
                    }
                    NativeTurboSchedManager.nativeIoctlDevice(handle, 6);
                    NativeTurboSchedManager.nativeCloseDevice(handle);
                    if (TurboSchedMonitor.getInstance().isDebugMode()) {
                        Trace.traceEnd(64L);
                    }
                }
            }
        }
    }

    private void enableGaea(boolean bEnable) {
        this.mGaeaEnable = bEnable;
        SystemProperties.set("persist.sys.turbosched.gaea.enable", Boolean.toString(bEnable));
        if (!bEnable) {
            boolean isSuccess = writeTurboSchedNode("GAEA", GAEA_PATH, "0");
            if (TurboSchedMonitor.getInstance().isDebugMode() && !isSuccess) {
                Slog.w(TAG, "failed to write gaea path node!");
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        boolean handled = parseDumpCommand(fd, pw, args);
        if (handled) {
            return;
        }
        dumpConfig(pw);
        pw.println("-----------------------history----------------------------");
        this.mHistoryLog.dump(pw);
    }

    private void dumpConfig(PrintWriter pw) {
        pw.println("--------------------current config-----------------------");
        String metisVersionSuffix = "";
        String str = this.mMetisVersion;
        if (str != null && !str.isEmpty()) {
            metisVersionSuffix = "-" + this.mMetisVersion;
        }
        pw.println("TurboSchedManagerService: v4.0.0" + metisVersionSuffix);
        pw.println(" disable cloud control : " + (!this.mReceiveCloudData));
        pw.println(" mTurboEnabled : " + this.mTurboEnabled + ", mTurboEnabledV2: " + this.mTurboEnabledV2 + ", mCloudControlEnabled : " + this.mCloudControlEnabled);
        pw.println(" CoreAppOptimizerEnabled: " + TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() + ", DebugMode: " + TurboSchedMonitor.getInstance().isDebugMode());
        pw.println(" CoreTop20AppOptimizerEnabled: " + this.mEnableTop20);
        pw.println(" mThermalBreakEnabled: " + this.mThermalBreakEnabled + ", mLinkEnable: " + this.mLinkEnable + ", mSupportGaea: " + this.mSupportGaea + ", mGaeaEnable: " + this.mGaeaEnable);
        pw.println(" TurboSchedConfig.mPolicyList : " + String.join(",", TurboSchedConfig.getPolicyList()));
        pw.println(" Caller allow list : " + Arrays.toString(this.mCallerAllowList.toArray()));
        pw.println(" Lock Contention Enabled : " + TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled());
        pw.println(" mDPSEnable : " + this.mDPSEnable);
        pw.println("--------------------environment config-----------------------");
        pw.println(" cores sched: " + SystemProperties.get("persist.sys.miui_animator_sched.big_prime_cores", "not set"));
        pw.println(" [PRendering] enable:" + SystemProperties.get("ro.vendor.perf.scroll_opt", "not set") + ", h-a value: " + SystemProperties.get("ro.vendor.perf.scroll_opt.heavy_app", "not set"));
        pw.println("--------------------kernel config-----------------------");
        if (this.mTurboNodeExist) {
            pw.println(" isFileAccess = " + TurboSchedUtil.checkFileAccess(TURBO_SCHED_PATH));
        } else {
            pw.println(" isFileAccess = " + TurboSchedUtil.checkFileAccess(ORI_TURBO_SCHED_PATH));
        }
        pw.println(" metis dev access = " + TurboSchedUtil.checkFileAccess(METIS_DEV_PATH));
        boolean isBWFPolicyAccess = TurboSchedUtil.checkFileAccess(TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH) && TurboSchedUtil.checkFileAccess(TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH);
        pw.println(" bwf policy access = " + isBWFPolicyAccess);
        boolean isPriorityPolicyAccess = TurboSchedUtil.checkFileAccess(TURBO_SCHED_PRIORITY_PATH) && TurboSchedUtil.checkFileAccess(TURBO_SCHED_CANCEL_PRIORITY_PATH);
        pw.println(" priority policy access = " + isPriorityPolicyAccess);
        boolean isLittleCorePolicyAccess = TurboSchedUtil.checkFileAccess(TURBO_SCHED_LITTLE_CORE_PATH) && TurboSchedUtil.checkFileAccess(TURBO_SCHED_CANCEL_LITTLE_CORE_PATH);
        pw.println(" little core policy access = " + isLittleCorePolicyAccess);
        pw.println(" thermal break enable access = " + TurboSchedUtil.checkFileAccess(TURBO_SCHED_THERMAL_BREAK_PATH));
        pw.println(" thermal break access = " + TurboSchedUtil.checkFileAccess(BOOST_THERMAL_PATH));
        pw.println(" link access =" + TurboSchedUtil.checkFileAccess(VIP_LINK_PATH));
        pw.println(" gaea access =" + TurboSchedUtil.checkFileAccess(GAEA_PATH));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:168:0x03a4, code lost:
    
        if (r1.equals("add") != false) goto L165;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v18 */
    /* JADX WARN: Type inference failed for: r2v19 */
    /* JADX WARN: Type inference failed for: r2v24 */
    /* JADX WARN: Type inference failed for: r2v79 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseDumpCommand(java.io.FileDescriptor r12, java.io.PrintWriter r13, java.lang.String[] r14) {
        /*
            Method dump skipped, instructions count: 1702
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.TurboSchedManagerService.parseDumpCommand(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    private void printCommandResult(PrintWriter pw, String result) {
        pw.println("--------------------command result-----------------------");
        pw.println(result);
    }

    private void recordRtPid(int id) {
        if (!TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled()) {
            return;
        }
        Slog.i(TAG, "recordrtpid,pid " + id + ", MITEST");
        synchronized (mTurboLock) {
            StringBuilder sb = new StringBuilder();
            sb.append(id);
            String str = sb.toString();
            boolean isSuccess = writeTurboSchedNode("RR-PID", RECORD_RT_PATH, str);
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.i(TAG, "recordrtpid, isSuccess:" + isSuccess + "pid " + id + ", MITEST");
            }
        }
    }

    private void enableFrameBoostInKernel(boolean enable) {
        boolean isSuccess = enable ? writeTurboSchedNode("TB-Enabled", BOOST_ENABLE_PATH, "1") : writeTurboSchedNode("TB-Enabled", BOOST_ENABLE_PATH, "0");
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.i(TAG, "enableFrameBoostInKernel, isSuccess:" + isSuccess);
        }
    }

    /* loaded from: classes.dex */
    private class TurboSchedBroadcastReceiver extends BroadcastReceiver {
        public TurboSchedBroadcastReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            TurboSchedManagerService.this.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON")) {
                TurboSchedManagerService.this.mScreenOn = true;
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                TurboSchedManagerService.this.mScreenOn = false;
            }
        }
    }

    private void enableTurboSched(boolean enable) {
        this.mTurboEnabled = enable;
        SystemProperties.set("persist.sys.turbosched.enable", Boolean.toString(enable));
    }

    protected void enableTurboSchedV2(boolean enable) {
        this.mTurboEnabledV2 = enable;
        SystemProperties.set("persist.sys.turbosched.enable_v2", Boolean.toString(enable));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean writeTidsToPath(String path, int[] tids) {
        StringBuilder sb = new StringBuilder();
        for (int i : tids) {
            sb.append(i);
            sb.append(":");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return writeTurboSchedNode("M-Tids", path, sb.toString());
    }

    private void setPolicyTimeoutChecker(final String logTag, int[] tids, long time, final String cancelPath, Map<Integer, Long> startTimeMap) {
        if (this.TurboSchedSDKTimer != null) {
            return;
        }
        for (int tid : tids) {
            startTimeMap.put(Integer.valueOf(tid), Long.valueOf(System.currentTimeMillis()));
        }
        this.TurboSchedSDKTimer = new Timer();
        TimerTask timerTask = new TimerTask() { // from class: com.miui.server.turbosched.TurboSchedManagerService.13
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                List<Integer> tidsList = new ArrayList<>();
                synchronized (TurboSchedManagerService.this.mTurboschedSdkTidsLock) {
                    Iterator<Map.Entry<String, TurboschedSDKParam>> iterator = TurboSchedManagerService.this.mTurboschedSDKParamMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, TurboschedSDKParam> entry = iterator.next();
                        String paramId = entry.getKey();
                        TurboschedSDKParam param = (TurboschedSDKParam) TurboSchedManagerService.this.mTurboschedSDKParamMap.get(paramId);
                        if (param != null && param.mStartTime + param.mSetTime <= System.currentTimeMillis()) {
                            tidsList.add(Integer.valueOf(param.mTid));
                            iterator.remove();
                        }
                    }
                    if (!tidsList.isEmpty()) {
                        int[] newTids = tidsList.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray();
                        boolean ret = TurboSchedManagerService.this.writeTidsToPath(cancelPath, newTids);
                        TurboSchedManagerService.this.mHistoryLog.log("TS [" + logTag + "] : success: " + ret + ", tids: " + Arrays.toString(newTids));
                    }
                    if (TurboSchedManagerService.this.mTurboschedSDKParamMap == null || TurboSchedManagerService.this.mTurboschedSDKParamMap.isEmpty()) {
                        if (TurboSchedManagerService.this.TurboSchedSDKTimerTask != null) {
                            TurboSchedManagerService.this.TurboSchedSDKTimerTask.cancel();
                            TurboSchedManagerService.this.TurboSchedSDKTimerTask = null;
                        }
                        if (TurboSchedManagerService.this.TurboSchedSDKTimer != null) {
                            TurboSchedManagerService.this.TurboSchedSDKTimer.cancel();
                            TurboSchedManagerService.this.TurboSchedSDKTimer = null;
                        }
                    }
                }
            }
        };
        this.TurboSchedSDKTimerTask = timerTask;
        this.TurboSchedSDKTimer.schedule(timerTask, 1000L, 1000L);
    }

    private String getThermalBreakString() {
        int temperatue = getBoardSensorTemperature();
        for (Map.Entry<Integer, String> entry : TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.entrySet()) {
            if (temperatue >= entry.getKey().intValue()) {
                return entry.getValue();
            }
        }
        return NO_THERMAL_BREAK_STRING;
    }

    private int getBoardSensorTemperature() {
        StringBuilder sb;
        int sensorTemp = 0;
        FileInputStream fis = null;
        try {
            try {
                fis = new FileInputStream(BOARD_TEMP_FILE);
                byte[] buffer = new byte[10];
                int read = fis.read(buffer);
                if (read > 0) {
                    String str = new String(buffer, 0, read);
                    sensorTemp = Integer.parseInt(str.trim()) / 1000;
                }
            } catch (Exception e) {
                Slog.e(TAG, "getBoardSensorTemperature failed : " + e.getMessage());
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e2) {
                        e = e2;
                        sb = new StringBuilder();
                        Slog.e(TAG, sb.append("getBoardSensorTemperature failed : ").append(e.getMessage()).toString());
                        return sensorTemp;
                    }
                }
            }
            try {
                fis.close();
            } catch (IOException e3) {
                e = e3;
                sb = new StringBuilder();
                Slog.e(TAG, sb.append("getBoardSensorTemperature failed : ").append(e.getMessage()).toString());
                return sensorTemp;
            }
            return sensorTemp;
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "getBoardSensorTemperature failed : " + e4.getMessage());
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TraceInfo {
        String desc;
        String name;
        long startTime;

        TraceInfo() {
        }
    }

    private void addCoreApp(List<String> coreAppList) {
        if (coreAppList == null) {
            return;
        }
        List<String> applist = new ArrayList<>(TurboSchedMonitor.getInstance().getCoreAppList());
        for (String app : coreAppList) {
            if (!applist.contains(app)) {
                applist.add(app);
            }
        }
        TurboSchedMonitor.getInstance().setCoreAppList(this.mContext, applist);
    }

    private void delCoreApp(List<String> coreAppList) {
        if (coreAppList == null) {
            return;
        }
        List<String> applist = new ArrayList<>(TurboSchedMonitor.getInstance().getCoreAppList());
        for (String app : coreAppList) {
            if (applist.contains(app)) {
                applist.remove(app);
            }
        }
        TurboSchedMonitor.getInstance().setCoreAppList(this.mContext, applist);
    }

    private void initTopAppList() {
        String appListStr = Settings.System.getString(this.mContext.getContentResolver(), "turbo_sched_core_app_list");
        if (appListStr != null) {
            this.mCoreAppsPackageNameList = Arrays.asList(appListStr.split(","));
        }
        TurboSchedMonitor.getInstance().setCoreAppList(this.mContext, this.mCoreAppsPackageNameList);
    }

    private List<String> initFrameTurboAppList() {
        List<String> appList = new ArrayList<>();
        String appListStr = SystemProperties.get("persist.sys.turbosched.frame_turbo.apps", "");
        if (appListStr != null && !appListStr.isEmpty()) {
            appList = new ArrayList<>(Arrays.asList(appListStr.split(",")));
        }
        Slog.d(TAG, "initFrameTurboAppList: " + appList);
        return appList;
    }

    private void syncTopAppList(List<String> coreAppList) {
        if (this.mIsGlobal) {
            this.mCoreAppsPackageNameList = DEFAULT_GL_APP_LIST;
        } else {
            if (!miui.os.Build.IS_TABLET) {
                if (this.mEnableTop20) {
                    this.mCoreAppsPackageNameList = DEFAULT_TOP20_APP_LIST;
                } else {
                    this.mCoreAppsPackageNameList = DEFAULT_APP_LIST;
                }
            } else if (this.mEnableTop20) {
                this.mCoreAppsPackageNameList = TABLET_TOP20_APP_LIST;
            } else {
                this.mCoreAppsPackageNameList = TABLET_TOP8_APP_LIST;
            }
            for (String app : coreAppList) {
                if (!this.mCoreAppsPackageNameList.contains(app)) {
                    this.mCoreAppsPackageNameList.add(app);
                }
            }
        }
        TurboSchedMonitor.getInstance().setCoreAppList(this.mContext, this.mCoreAppsPackageNameList);
    }

    private void syncTop20AppList(boolean optimizerEnabled) {
        this.mEnableTop20 = optimizerEnabled;
        SystemProperties.set("persist.sys.turbosched.enabletop20app", Boolean.toString(optimizerEnabled));
        if (!miui.os.Build.IS_TABLET) {
            List<String> apps = getCoreAppList();
            syncTopAppList(apps);
        } else {
            List<String> apps2 = TABLET_TOP8_APP_LIST;
            syncTopAppList(apps2);
        }
    }

    private boolean isForeground(String packageName) {
        return packageName.equals(this.mProcessName);
    }

    public void onCoreAppFirstActivityStart(String processName) {
        if (this.mSupportGaea && this.mGaeaEnable && this.mCoreAppsPackageNameList.contains(processName)) {
            this.enableGaeaAppProcessName = processName;
            boolean isSuccess = writeTurboSchedNode("GAEA", GAEA_PATH, "1");
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.w(TAG, "write gaea path node 1: " + isSuccess);
            }
        }
        if (this.mLinkEnable && processName != null && this.mAppsLinkVipList.contains(processName)) {
            this.enableLinkVipAppProcessName = processName;
            boolean isSuccess2 = writeTurboSchedNode("LINK", VIP_LINK_PATH, "1");
            if (TurboSchedMonitor.getInstance().isDebugMode() && !isSuccess2) {
                Slog.w(TAG, "LinkVip, write linkvip path node 1:" + isSuccess2);
            }
        }
    }

    public void onCoreAppLastActivityStop(String processName) {
        String str;
        String str2;
        if (this.mSupportGaea && this.mGaeaEnable && this.mCoreAppsPackageNameList.contains(processName) && (str2 = this.enableGaeaAppProcessName) != null && str2.equals(processName)) {
            boolean isSuccess = writeTurboSchedNode("GAEA", GAEA_PATH, "0");
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.w(TAG, "write gaea path node 0: " + isSuccess);
            }
        }
        if (this.mLinkEnable && processName != null && this.mAppsLinkVipList.contains(processName) && (str = this.enableLinkVipAppProcessName) != null && str.equals(processName)) {
            boolean isSuccess2 = writeTurboSchedNode("LINK", VIP_LINK_PATH, "0");
            if (TurboSchedMonitor.getInstance().isDebugMode() && !isSuccess2) {
                Slog.w(TAG, "LinkVip, write linkvip path node 0:" + isSuccess2);
            }
        }
    }

    public void setForegroundProcessName(String processName) {
        this.mProcessName = processName;
    }
}
