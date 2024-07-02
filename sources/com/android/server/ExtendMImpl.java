package com.android.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IVold;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.security.AccessControlImpl;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.ExtendMStub$$")
/* loaded from: classes.dex */
public class ExtendMImpl extends JobService implements ExtendMStub {
    private static final String BDEV_SYS = "/sys/block/zram0/backing_dev";
    private static final String BROADCASTS_ACTION_MARK = "miui.extm.action.mark";
    private static final String CLOUD_DM_OPT_PROP = "cloud_dm_opt_enable";
    private static final String CLOUD_EXT_MEM_PROP = "cloud_extm_percent";
    private static final String CLOUD_MFZ_PROP = "cloud_memFreeze_control";
    private static final long DELAY_TIME = 1000;
    private static final String EXTM_UUID = "extm_uuid";
    private static final String MEMINFO_MEM_TOTAL = "MemTotal";
    private static final String MEMINFO_SWAP_FREE = "SwapFree";
    private static final String MEMINFO_SWAP_TOTAL = "SwapTotal";
    private static final String MFZ_ENABLE_SYS = "/sys/block/zram0/mfz_enable";
    private static final int MM_STATS_MAX_FILE_SIZE = 128;
    private static final String MM_STATS_SYS = "/sys/block/zram0/mm_stat";
    private static final int MSG_DO_FLUSH_HIGH = 3;
    private static final int MSG_DO_FLUSH_LOW = 1;
    private static final int MSG_DO_FLUSH_MEDIUM = 2;
    private static final int MSG_DO_MARK = 6;
    private static final int MSG_START_EXTM = 5;
    private static final int MSG_STOP_FLUSH = 4;
    private static final String OLD_COUNT_SYS = "/sys/block/zram0/idle_stat";
    private static final int REPORTMEMP_EXTM_LIMIT = 3;
    private static final int RESET_WB_LIMIT_JOB_ID = 33524;
    private static final int STATE_NOT_RUNNING = 0;
    private static final int STATE_WAIT_FOR_MARK = 1;
    private static final int STATE_WAIT_FOR_RESUME_FLUSH = 3;
    private static final String TAG = "ExtM";
    private static final String TAG_AM = "MFZ";
    private static final long THIRTY_SECONDS = 30000;
    private static final long THREE_MINUTES = 180000;
    private static final String VERSION = "3.0";
    private static final String WB_LIMIT_ENABLE_SYS = "/sys/block/zram0/writeback_limit_enable";
    private static final String WB_LIMIT_SYS = "/sys/block/zram0/writeback_limit";
    private static final int WB_STATS_MAX_FILE_SIZE = 128;
    private static final String WB_STATS_SYS = "/sys/block/zram0/bd_stat";
    private static final String WB_SYS_LIMIT_PAGES = "persist.miui.extm.daily_flush_count";
    private static ExtendMRecord sExtendMRecord;
    private static ArrayList<String> sMemTags;
    public static ExtendMImpl sSelf;
    private IBinder binder;
    private Context mContext;
    private volatile IVold mVold;
    private static final boolean LOG_VERBOSE = SystemProperties.getBoolean("persist.miui.extm.debug_enable", false);
    private static final String EXTM_SETTINGS_PROP = SystemProperties.get("persist.miui.extm.enable", "0");
    private static final boolean MEMORY_FREEZE_ENABLE = SystemProperties.getBoolean("persist.sys.mfz.enable", false);
    private static final boolean MIUI_DM_OPT_ENABLE = SystemProperties.getBoolean("persist.miui.extm.dm_opt.enable", false);
    private static final ComponentName sExtendM = new ComponentName("android", ExtendMImpl.class.getName());
    private static final int FLUSH_LEVEL = SystemProperties.getInt("persist.lm.em.flush_level", 5);
    private static final long MARK_DURATION = SystemProperties.getLong("persist.lm.em.mark_duration", 120000);
    private static final long FLUSH_DURATION = SystemProperties.getLong("persist.lm.em.flush_duration", 600000);
    private static final long DELAY_DURATION = SystemProperties.getLong("persist.lm.em.delay_duration", 30000);
    private static final long FLUSH_INTERVAL_LIMIT = SystemProperties.getLong("persist.lm.em.flush_interval_limit", 30000);
    private static final int PER_FLUSH_QUOTA_LOW = SystemProperties.getInt("persist.lm.em.per_flush_quota_low", 5000);
    private static final int PER_FLUSH_QUOTA_MEDIUM = SystemProperties.getInt("persist.lm.em.per_flush_quota_medium", 3000);
    private static final int PER_FLUSH_QUOTA_HIGH = SystemProperties.getInt("persist.lm.em.per_flush_quota_high", 2000);
    private static final int FLUSH_ZRAM_USEDRATE = SystemProperties.getInt("persist.lm.em.flush_zram_usedrate", 50);
    private static final int FLUSH_RWTHRESHOLD_PART = SystemProperties.getInt("persist.lm.em.flush_rwthreshold_part", 3);
    private static final int FLUSH_LOW_LEVEL = SystemProperties.getInt("persist.em.low_flush_level", 10);
    private static final int FLUSH_MEDIUM_LEVEL = SystemProperties.getInt("persist.em.medium_flush_level", 8);
    private static final int FLUSH_HIGH_LEVEL = SystemProperties.getInt("persist.em.high_flush_level", 5);
    private static final String MEM_CONF_FILE_PATH = SystemProperties.get("persist.miui.extm.memory_conf_file", "/system_ext/etc/perfinit_bdsize_zram.conf");
    private static ArrayList<Float> extmGear = new ArrayList<>();
    private int mCurState = 0;
    private boolean mLocalCloudEnable = false;
    private boolean mfzLocalCloudEnable = false;
    private boolean dmoptLocalCloudEnable = false;
    private int mLastOldPages = 0;
    private long mTotalMarked = 0;
    private long mTotalFlushed = 0;
    private long lastFlushTime = 0;
    private int flush_level_adjust = 10;
    private int flush_number_adjust = 0;
    private int flush_per_quota = 0;
    private int prev_bd_write = 0;
    private int prev_bd_read = 0;
    private Map<String, Long> mMemInfo = new HashMap();
    private long lastReportMPtime = 0;
    private int reportMemPressureCnt = 0;
    private int kernelSupportCheckCnt = 0;
    private HandlerThread mThread = new HandlerThread(TAG);
    private boolean isInitSuccess = false;
    private MyHandler mHandler = null;
    private boolean hasParseMemConfSuccessful = false;
    private long mScreenOnTime = 0;
    private long mMarkStartTime = 0;
    private long mMarkDiffTime = 0;
    private long mFlushDiffTime = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.ExtendMImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                if (ExtendMImpl.LOG_VERBOSE) {
                    Slog.d(ExtendMImpl.TAG, "-----------------------");
                    Slog.d(ExtendMImpl.TAG, "Screen off");
                }
                long mScreenOffTime = System.currentTimeMillis();
                if (mScreenOffTime - ExtendMImpl.this.mScreenOnTime > ExtendMImpl.THREE_MINUTES) {
                    ExtendMImpl extendMImpl = ExtendMImpl.this;
                    extendMImpl.mMarkDiffTime = (extendMImpl.mMarkDiffTime + (mScreenOffTime - ExtendMImpl.this.mMarkStartTime)) % ExtendMImpl.MARK_DURATION;
                    ExtendMImpl.this.mFlushDiffTime += mScreenOffTime - ExtendMImpl.this.mScreenOnTime;
                    Slog.i(ExtendMImpl.TAG, "mark: already wait " + (ExtendMImpl.this.mMarkDiffTime / AccessControlImpl.LOCK_TIME_OUT) + "m flush: already wait " + (ExtendMImpl.this.mFlushDiffTime / AccessControlImpl.LOCK_TIME_OUT) + "m");
                } else {
                    Slog.i(ExtendMImpl.TAG, "Screen on--> Screen off diff < 2 minutes");
                }
                ExtendMImpl.this.unregisterAlarmClock();
                if (ExtendMImpl.this.mFlushDiffTime >= ExtendMImpl.FLUSH_DURATION || ExtendMImpl.this.mCurState == 3) {
                    Slog.i(ExtendMImpl.TAG, "screen on up to " + (ExtendMImpl.FLUSH_DURATION / AccessControlImpl.LOCK_TIME_OUT) + "m , try to flush.");
                    ExtendMImpl.this.updateState(1);
                    ExtendMImpl.this.tryToFlushPages();
                    ExtendMImpl.this.mFlushDiffTime = 0L;
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                ExtendMImpl.this.tryToStopFlush();
                ExtendMImpl.this.mScreenOnTime = System.currentTimeMillis();
                if (ExtendMImpl.this.getQuotaLeft() > 0) {
                    if (ExtendMImpl.LOG_VERBOSE) {
                        Slog.d(ExtendMImpl.TAG, "registerAlarmClock: " + ((ExtendMImpl.MARK_DURATION - ExtendMImpl.this.mMarkDiffTime) / AccessControlImpl.LOCK_TIME_OUT) + " m");
                    }
                    ExtendMImpl.this.registerAlarmClock(System.currentTimeMillis() + (ExtendMImpl.MARK_DURATION - ExtendMImpl.this.mMarkDiffTime));
                    return;
                }
                return;
            }
            if (ExtendMImpl.BROADCASTS_ACTION_MARK.equals(action)) {
                Slog.i(ExtendMImpl.TAG, "mark duration up to " + (ExtendMImpl.MARK_DURATION / AccessControlImpl.LOCK_TIME_OUT) + "m , try to mark.");
                Slog.d(ExtendMImpl.TAG_AM, "Start Mark");
                ExtendMImpl.this.mHandler.sendEmptyMessage(6);
                try {
                    String idle_stats = FileUtils.readTextFile(new File(ExtendMImpl.OLD_COUNT_SYS), 128, "");
                    Slog.d(ExtendMImpl.TAG_AM, "Mark Finshed ! idle_stats: " + idle_stats);
                } catch (IOException e) {
                    Slog.d(ExtendMImpl.TAG_AM, "idle_stats: error");
                }
                ExtendMImpl.this.mMarkDiffTime = 0L;
                ExtendMImpl.this.mMarkStartTime = System.currentTimeMillis();
                if (ExtendMImpl.LOG_VERBOSE) {
                    SimpleDateFormat mFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String date = mFmt.format(new Date(ExtendMImpl.this.mMarkStartTime));
                    Slog.i(ExtendMImpl.TAG, "mMarkStartTime: " + date + " ,reset it");
                }
                if (ExtendMImpl.this.getQuotaLeft() > 0) {
                    ExtendMImpl.this.registerAlarmClock(System.currentTimeMillis() + ExtendMImpl.MARK_DURATION);
                }
            }
        }
    };
    IBinder.DeathRecipient death = new IBinder.DeathRecipient() { // from class: com.android.server.ExtendMImpl.2
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.w(ExtendMImpl.TAG, "vold died; reconnecting");
            ExtendMImpl.this.mVold = null;
            ExtendMImpl.this.connect();
        }
    };
    private boolean isFlushFinished = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ExtendMImpl> {

        /* compiled from: ExtendMImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ExtendMImpl INSTANCE = new ExtendMImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ExtendMImpl m66provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ExtendMImpl m65provideNewInstance() {
            return new ExtendMImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sMemTags = arrayList;
        arrayList.add(MEMINFO_SWAP_TOTAL);
        sMemTags.add(MEMINFO_SWAP_FREE);
        sMemTags.add(MEMINFO_MEM_TOTAL);
    }

    public ExtendMImpl() {
    }

    public ExtendMImpl(Context context) {
        this.mContext = context;
    }

    public void start(Context context) {
        Slog.i(TAG, "Extm Version 3.0");
        getInstance(context);
        if (this.mContext == null) {
            this.mContext = context;
        }
        this.mThread.start();
        MyHandler myHandler = new MyHandler(this.mThread.getLooper());
        this.mHandler = myHandler;
        myHandler.sendEmptyMessageDelayed(5, 1000L);
    }

    private int getCloudPercent() {
        try {
            int percent = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOUD_EXT_MEM_PROP, -2);
            return percent;
        } catch (Settings.SettingNotFoundException e) {
            Slog.i(TAG, "Do not get local cloud percent, set as 9");
            return 9;
        }
    }

    private int getMfzCloud() {
        try {
            int mfzcloud = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOUD_MFZ_PROP, -2);
            return mfzcloud;
        } catch (Settings.SettingNotFoundException e) {
            Slog.i(TAG_AM, "continue mfz cloud control");
            return 1;
        }
    }

    private int getdmoptCloud() {
        try {
            int dmoptcloud = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOUD_DM_OPT_PROP, -2);
            return dmoptcloud;
        } catch (Settings.SettingNotFoundException e) {
            Slog.i(TAG, "continue dmopt cloud control");
            return 1;
        }
    }

    public boolean isCloudAllowed() {
        return this.mLocalCloudEnable;
    }

    public boolean isMfzCloud() {
        return this.mfzLocalCloudEnable;
    }

    public boolean isdmoptCloud() {
        return this.dmoptLocalCloudEnable;
    }

    private boolean isKernelSupported() {
        try {
            String backingDev = FileUtils.readTextFile(new File(BDEV_SYS), 128, "");
            if (!"none".equals(backingDev.trim())) {
                return true;
            }
            return false;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to read kernel info");
            return false;
        }
    }

    private boolean isMfzSupported() {
        File MFZ_sys = new File(MFZ_ENABLE_SYS);
        if (!MFZ_sys.exists()) {
            Slog.d(TAG_AM, "Do not supported MFZ");
            return false;
        }
        Slog.d(TAG_AM, "Supported MFZ");
        return true;
    }

    private void init() {
        connect();
        this.mMarkStartTime = System.currentTimeMillis();
        this.mScreenOnTime = System.currentTimeMillis();
        getExtendMRecordInstance();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(BROADCASTS_ACTION_MARK);
        this.mContext.registerReceiver(this.mReceiver, filter);
        Slog.i(TAG, "init: register mark monitor");
        enableFlushQuota();
        JobScheduler js = (JobScheduler) this.mContext.getSystemService("jobscheduler");
        js.schedule(new JobInfo.Builder(RESET_WB_LIMIT_JOB_ID, sExtendM).build());
        this.mCurState = 1;
        this.isInitSuccess = true;
    }

    private static ExtendMRecord getExtendMRecordInstance() {
        if (sExtendMRecord == null) {
            sExtendMRecord = new ExtendMRecord();
        }
        return sExtendMRecord;
    }

    public static ExtendMImpl getInstance(Context context) {
        ExtendMImpl extendMImpl;
        if (context == null) {
            return null;
        }
        synchronized (ExtendMImpl.class) {
            if (sSelf == null) {
                sSelf = new ExtendMImpl(context);
            }
            extendMImpl = sSelf;
        }
        return extendMImpl;
    }

    private void exit() {
        this.mContext.unregisterReceiver(this.mReceiver);
        Slog.i(TAG, "exit: unregister screen on/off monitor");
        disconnect();
        Slog.i(TAG, "exit: disconnect vold");
        this.mCurState = 0;
        this.isInitSuccess = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshExtmSettings() {
        Slog.i(TAG, "Enter cloud control");
        parseExtMemCloudControl();
        if (isCloudAllowed()) {
            if (this.mCurState == 0) {
                init();
            } else {
                Slog.i(TAG, "Already running");
            }
        } else if (this.mCurState != 0) {
            exit();
        } else {
            Slog.i(TAG, "Already stopped");
        }
        Slog.i(TAG, "Exit cloud control");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ParseMfzSettings() {
        Slog.i(TAG_AM, "Enter mfz cloud control");
        int mfzCloudEnable = getMfzCloud();
        if (mfzCloudEnable < 0 || mfzCloudEnable > 1) {
            Slog.i(TAG_AM, "Invalid cloud control enable");
            return;
        }
        if (mfzCloudEnable == 0) {
            this.mfzLocalCloudEnable = false;
        } else {
            this.mfzLocalCloudEnable = true;
        }
        Slog.d(TAG_AM, "CloudEnable: " + this.mfzLocalCloudEnable + ", Exit mfz cloud control");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ParsedmoptSettings() {
        Slog.i(TAG, "Enter dmopt cloud control");
        int dmoptCloudEnable = getdmoptCloud();
        if (dmoptCloudEnable < 0 || dmoptCloudEnable > 1) {
            Slog.i(TAG, "Invalid cloud control enable");
            return;
        }
        if (dmoptCloudEnable == 0) {
            this.dmoptLocalCloudEnable = false;
        } else {
            this.dmoptLocalCloudEnable = true;
        }
        Slog.d(TAG, "CloudEnable:" + this.dmoptLocalCloudEnable + ",Exit dmopt cloud control");
    }

    private int getRefinedUUID() {
        ContentResolver cr = this.mContext.getContentResolver();
        String uuid = Settings.Global.getString(cr, EXTM_UUID);
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            Settings.Global.putString(cr, EXTM_UUID, uuid);
        }
        int sum = 0;
        for (int i = 0; i < uuid.length(); i++) {
            sum += uuid.charAt(i) - '0';
        }
        int i2 = sum % 10;
        return (i2 + 10) % 10;
    }

    private void parseExtMemCloudControl() {
        int cloudPercent = getCloudPercent();
        if (cloudPercent < 0 || cloudPercent >= 10) {
            Slog.i(TAG, "Invalid clound control value: " + cloudPercent + ", must be in the range [0,9]");
            this.mLocalCloudEnable = false;
            return;
        }
        int thres = getRefinedUUID();
        Slog.i(TAG, "ExtMemCloudControl value is " + cloudPercent + ", thres is " + thres);
        if (cloudPercent == 9 || cloudPercent > thres) {
            this.mLocalCloudEnable = true;
        } else {
            this.mLocalCloudEnable = false;
        }
    }

    private long pagesToMb(long pageCnt) {
        return pageCnt / 256;
    }

    /* JADX WARN: Incorrect condition in loop: B:11:0x0034 */
    /* JADX WARN: Incorrect condition in loop: B:17:0x0047 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void getPagesNeedFlush(int r10) {
        /*
            r9 = this;
            java.lang.String r0 = "ExtM"
            r1 = 0
            r9.flush_number_adjust = r1
            r2 = 10
            r9.flush_level_adjust = r2
            boolean r2 = r9.isFlushthrashing()
            if (r2 == 0) goto L15
            int r10 = com.android.server.ExtendMImpl.FLUSH_LOW_LEVEL
            int r2 = com.android.server.ExtendMImpl.PER_FLUSH_QUOTA_LOW
            r9.flush_per_quota = r2
        L15:
            java.io.File r2 = new java.io.File     // Catch: java.io.IOException -> L94
            java.lang.String r3 = "/sys/block/zram0/idle_stat"
            r2.<init>(r3)     // Catch: java.io.IOException -> L94
            java.lang.String r3 = ""
            java.lang.String r1 = android.os.FileUtils.readTextFile(r2, r1, r3)     // Catch: java.io.IOException -> L94
            if (r1 != 0) goto L25
            return
        L25:
            java.lang.String r2 = "\\s+"
            java.lang.String[] r2 = r1.split(r2)     // Catch: java.io.IOException -> L94
            r3 = 0
            r4 = 0
            r5 = 0
            r6 = 0
            int r7 = r2.length     // Catch: java.io.IOException -> L94
            int r7 = r7 + (-1)
        L32:
            int r4 = r10 + (-1)
            if (r7 < r4) goto L42
            if (r7 < 0) goto L42
            r4 = r2[r7]     // Catch: java.io.IOException -> L94
            int r4 = java.lang.Integer.parseInt(r4)     // Catch: java.io.IOException -> L94
            int r6 = r6 + r4
            int r7 = r7 + (-1)
            goto L32
        L42:
            int r4 = r2.length     // Catch: java.io.IOException -> L94
            int r4 = r4 + (-1)
        L45:
            int r7 = r10 + (-1)
            if (r4 < r7) goto L5d
            if (r4 < 0) goto L5d
            r7 = r2[r4]     // Catch: java.io.IOException -> L94
            int r7 = java.lang.Integer.parseInt(r7)     // Catch: java.io.IOException -> L94
            r5 = r7
            if (r5 <= 0) goto L5a
            int r3 = r3 + r5
            int r7 = r9.flush_per_quota     // Catch: java.io.IOException -> L94
            if (r3 <= r7) goto L5a
            goto L5d
        L5a:
            int r4 = r4 + (-1)
            goto L45
        L5d:
            int r7 = r10 + (-1)
            if (r4 <= r7) goto L66
            int r7 = r4 + 1
            r9.flush_level_adjust = r7     // Catch: java.io.IOException -> L94
            goto L68
        L66:
            r9.flush_level_adjust = r10     // Catch: java.io.IOException -> L94
        L68:
            int r7 = r9.flush_per_quota     // Catch: java.io.IOException -> L94
            if (r3 <= r7) goto L6d
            goto L6e
        L6d:
            r7 = r3
        L6e:
            r9.flush_number_adjust = r7     // Catch: java.io.IOException -> L94
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch: java.io.IOException -> L94
            r7.<init>()     // Catch: java.io.IOException -> L94
            java.lang.String r8 = "can_flush_idle_pages:"
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.io.IOException -> L94
            java.lang.StringBuilder r7 = r7.append(r6)     // Catch: java.io.IOException -> L94
            java.lang.String r8 = " flush_number_adjust: "
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.io.IOException -> L94
            int r8 = r9.flush_number_adjust     // Catch: java.io.IOException -> L94
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.io.IOException -> L94
            java.lang.String r7 = r7.toString()     // Catch: java.io.IOException -> L94
            android.util.Slog.e(r0, r7)     // Catch: java.io.IOException -> L94
            goto L9a
        L94:
            r1 = move-exception
            java.lang.String r2 = "Failed to get pages need to flush"
            android.util.Slog.e(r0, r2)
        L9a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ExtendMImpl.getPagesNeedFlush(int):void");
    }

    private int getFlushedPages() {
        try {
            String wbStats = FileUtils.readTextFile(new File(WB_STATS_SYS), 128, "");
            return Integer.parseInt(wbStats.trim().split("\\s+")[2], 10);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read flushed page count");
            return -1;
        }
    }

    private String getReadBackRate() {
        try {
            String wbStats = FileUtils.readTextFile(new File(WB_STATS_SYS), 128, "");
            String[] wbStat = wbStats.trim().split("\\s+");
            BigDecimal b1 = new BigDecimal(wbStat[1]);
            BigDecimal b2 = new BigDecimal(wbStat[2]);
            if (b2.intValue() == 0) {
                return "Failed to get read back rate";
            }
            Double result = Double.valueOf(b1.divide(b2, 2, 4).doubleValue());
            return String.format("%.2f%%", Double.valueOf(result.doubleValue() * 100.0d));
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read flushed page count");
            return " calculate read back rate error";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void readMemInfo() {
        Map<String, Long> result;
        StrictMode.ThreadPolicy oldPolicy;
        try {
            this.mMemInfo = null;
            result = new HashMap<>();
            oldPolicy = StrictMode.allowThreadDiskReads();
        } catch (Throwable th) {
            th = th;
        }
        try {
            FileReader fileReader = new FileReader("/proc/meminfo");
            try {
                BufferedReader reader = new BufferedReader(fileReader);
                while (true) {
                    try {
                        String line = reader.readLine();
                        try {
                            if (line == null) {
                                break;
                            }
                            try {
                                try {
                                    String[] items = line.split(":");
                                    if (sMemTags.contains(items[0].trim())) {
                                        result.put(items[0].trim(), Long.valueOf(items[1].substring(0, items[1].indexOf("k")).trim()));
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    fileReader.close();
                                    throw th;
                                }
                            } catch (Exception e) {
                                reader.close();
                                fileReader.close();
                                StrictMode.setThreadPolicy(oldPolicy);
                                return;
                            } catch (Throwable th3) {
                                th = th3;
                                reader.close();
                                throw th;
                            }
                        } catch (Exception e2) {
                            e = e2;
                            Slog.e(TAG, "Cannot readMemInfo from /proc/meminfo", e);
                            StrictMode.setThreadPolicy(oldPolicy);
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
                this.mMemInfo = result;
                reader.close();
                fileReader.close();
                StrictMode.setThreadPolicy(oldPolicy);
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (Exception e3) {
            e = e3;
        } catch (Throwable th6) {
            th = th6;
            StrictMode.setThreadPolicy(oldPolicy);
            throw th;
        }
    }

    private boolean isFlushthrashing() {
        int cur_bd_write = 0;
        int cur_bd_read = 0;
        boolean bThrashing = false;
        try {
            String wbStats = FileUtils.readTextFile(new File(WB_STATS_SYS), 128, "");
            String[] wbStat = wbStats.trim().split("\\s+");
            cur_bd_read = Integer.parseInt(wbStat[1]);
            cur_bd_write = Integer.parseInt(wbStat[2]);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read bd_stat");
        }
        if (cur_bd_write != 0 && cur_bd_read != 0) {
            int i = this.prev_bd_write;
            if (cur_bd_write - i > 0) {
                int bdRWRate = ((cur_bd_read - this.prev_bd_read) * 100) / (cur_bd_write - i);
                if (bdRWRate > FLUSH_RWTHRESHOLD_PART) {
                    Slog.e(TAG, "ExtM flush exist thrashing");
                    bThrashing = true;
                }
            }
            this.prev_bd_read = cur_bd_read;
            this.prev_bd_write = cur_bd_write;
            return bThrashing;
        }
        return false;
    }

    private synchronized int getZramUseRate() {
        readMemInfo();
        Map<String, Long> map = this.mMemInfo;
        if (map != null && map.get(MEMINFO_SWAP_TOTAL) != null && this.mMemInfo.get(MEMINFO_SWAP_FREE) != null) {
            long swapTotal = this.mMemInfo.get(MEMINFO_SWAP_TOTAL).longValue();
            long swapFree = this.mMemInfo.get(MEMINFO_SWAP_FREE).longValue();
            if (swapTotal == 0) {
                return 0;
            }
            int zramUsedRate = (int) (((swapTotal - swapFree) * 100) / swapTotal);
            if (LOG_VERBOSE) {
                Slog.d(TAG, "getZramUseRate zramUsedRate:" + zramUsedRate);
            }
            return zramUsedRate;
        }
        return 0;
    }

    private String readerJsonFromFile(File file) {
        if (file == null) {
            return null;
        }
        try {
            FileReader fileReader = new FileReader(file);
            Reader reader = new InputStreamReader(new FileInputStream(file), "Utf-8");
            StringBuffer sb = new StringBuffer();
            while (true) {
                int ch = reader.read();
                if (ch != -1) {
                    sb.append((char) ch);
                } else {
                    fileReader.close();
                    reader.close();
                    String jsonStr = sb.toString();
                    return jsonStr;
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "file read error: " + e);
            return null;
        }
    }

    private int getDataSizeGB() {
        StatFs statFs = new StatFs("/data");
        long flashTotal = statFs.getTotalBytes() / FormatBytesUtil.GB;
        long gap = 16;
        if (flashTotal > 32 && flashTotal <= 64) {
            gap = 32;
        } else if (flashTotal > 64 && flashTotal <= 256) {
            gap = 64;
        } else if (flashTotal > 256 && flashTotal <= FormatBytesUtil.KB) {
            gap = 128;
        } else if (flashTotal > FormatBytesUtil.KB) {
            gap = 256;
        }
        return (int) (((flashTotal / gap) + 1) * gap);
    }

    private int getMemSizeGB() {
        readMemInfo();
        long memTotal = this.mMemInfo.get(MEMINFO_MEM_TOTAL).longValue() / FormatBytesUtil.MB;
        long gap = 1;
        if (memTotal > 4 && memTotal <= 8) {
            gap = 2;
        } else if (memTotal > 8) {
            gap = 4;
        }
        return (int) (((memTotal / gap) + 1) * gap);
    }

    private float[] getDefaultExtMGear(int memSize, int dataSize) {
        float[] ret = new float[3];
        if (memSize >= 8 && dataSize >= 128) {
            ret[0] = 4.0f;
            ret[1] = 6.0f;
            ret[2] = 8.0f;
        } else {
            ret[0] = 1.0f;
            ret[1] = 2.0f;
            ret[2] = 3.0f;
        }
        return ret;
    }

    private float[] changeArrayListToFloatArray(ArrayList<Float> arrayList) {
        float[] ret = new float[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            ret[i] = arrayList.get(i).floatValue();
        }
        Slog.d(TAG, "arrayList value is: " + Arrays.toString(ret));
        return ret;
    }

    public float[] getExtMGear() {
        int count;
        float value;
        File jsonFile;
        String jsonData;
        if (extmGear.size() > 0 && this.hasParseMemConfSuccessful) {
            Slog.d(TAG, "extmGear exist");
            return changeArrayListToFloatArray(extmGear);
        }
        int flashSizeGB = getDataSizeGB();
        int memSizeGB = getMemSizeGB();
        Slog.d(TAG, "flashSize(GB): " + flashSizeGB + ", memSize(GB):" + memSizeGB);
        File jsonFile2 = new File(MEM_CONF_FILE_PATH);
        String jsonData2 = readerJsonFromFile(jsonFile2);
        if (jsonData2 == null) {
            Slog.e(TAG, "No json data.");
            return getDefaultExtMGear(memSizeGB, flashSizeGB);
        }
        try {
            extmGear.clear();
            JSONObject parse = new JSONObject(jsonData2);
            JSONArray autoZramItems = parse.getJSONArray("auto_zram");
            int i = 0;
            while (i < autoZramItems.length()) {
                JSONObject autoZramItem = (JSONObject) autoZramItems.get(i);
                JSONArray autoZramFlashItems = autoZramItem.getJSONArray("auto_zram_flash");
                int count2 = 0;
                while (true) {
                    if (count2 >= autoZramFlashItems.length()) {
                        count = 0;
                        break;
                    }
                    try {
                        if (autoZramFlashItems.getInt(count2) != flashSizeGB) {
                            count2++;
                        } else {
                            count = 1;
                            break;
                        }
                    } catch (JSONException e) {
                        e = e;
                        Slog.e(TAG, "Json parse error: " + e);
                        return getDefaultExtMGear(memSizeGB, flashSizeGB);
                    }
                }
                if (count != 0) {
                    String autoZramRamTag = "auto_zram_ram_" + memSizeGB + "G";
                    JSONObject autoZramRamTagItems = autoZramItem.getJSONObject(autoZramRamTag);
                    Set<String> keys = autoZramRamTagItems.keySet();
                    for (String key : keys) {
                        try {
                            value = Float.parseFloat(key);
                            jsonFile = jsonFile2;
                            try {
                                jsonData = jsonData2;
                            } catch (JSONException e2) {
                                e = e2;
                            }
                        } catch (NumberFormatException e3) {
                            jsonFile2 = jsonFile2;
                            jsonData2 = jsonData2;
                        }
                        try {
                            extmGear.add(Float.valueOf(value));
                            jsonFile2 = jsonFile;
                            jsonData2 = jsonData;
                        } catch (JSONException e4) {
                            e = e4;
                            Slog.e(TAG, "Json parse error: " + e);
                            return getDefaultExtMGear(memSizeGB, flashSizeGB);
                        }
                    }
                }
                i++;
                jsonFile2 = jsonFile2;
                jsonData2 = jsonData2;
            }
            if (extmGear.size() > 0) {
                float[] ret = changeArrayListToFloatArray(extmGear);
                this.hasParseMemConfSuccessful = true;
                Slog.d(TAG, "read extm gear success and gear is: " + Arrays.toString(ret));
                return ret;
            }
            return getDefaultExtMGear(memSizeGB, flashSizeGB);
        } catch (JSONException e5) {
            e = e5;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getQuotaLeft() {
        try {
            String remainingPages = FileUtils.readTextFile(new File(WB_LIMIT_SYS), 0, "");
            return Integer.parseInt(remainingPages.trim());
        } catch (IOException e) {
            Slog.e(TAG, "Failed to get remaining pages");
            return -1;
        }
    }

    private void markPagesAsNew() {
    }

    private void enableFlushQuota() {
        try {
            FileUtils.stringToFile(new File(WB_LIMIT_ENABLE_SYS), "1");
        } catch (IOException e) {
            Slog.e(TAG, "Failed to enable flush quota ");
        }
    }

    private void setFlushQuota() {
        try {
            String WriteBackLimit = SystemProperties.get(WB_SYS_LIMIT_PAGES);
            FileUtils.stringToFile(new File(WB_LIMIT_SYS), WriteBackLimit);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to set flush quota");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void SetMfzEnable() {
        try {
            int MFZ_SYS_Control = getMfzCloud();
            SystemProperties.set("persist.sys.mfz.enable", String.valueOf(isMfzCloud()));
            FileUtils.stringToFile(new File(MFZ_ENABLE_SYS), String.valueOf(MFZ_SYS_Control));
        } catch (IOException e) {
            Slog.e(TAG_AM, "Failed to set mfz sys");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void SetdmoptEnable() {
        try {
            if (MIUI_DM_OPT_ENABLE) {
                SystemProperties.set("persist.miui.extm.dm_opt.enable", String.valueOf(isdmoptCloud()));
            }
        } catch (Exception e) {
            Slog.e(TAG_AM, "Failed to set dmopt sys");
        }
    }

    private static void resetNextFlushQuota(Context context) {
        Calendar calendar = tomorrowMidnight();
        long timeToMidnight = calendar.getTimeInMillis() - System.currentTimeMillis();
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        js.schedule(new JobInfo.Builder(RESET_WB_LIMIT_JOB_ID, sExtendM).setMinimumLatency(timeToMidnight).build());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAndSaveFlushedStat(int prevCount, long startTime) {
        if (prevCount != -1) {
            int writeCount = getFlushedPages() - prevCount;
            String mReadBackRate = getReadBackRate();
            saveFlushData(writeCount, mReadBackRate, startTime);
            this.mTotalFlushed += writeCount;
            Slog.i(TAG, writeCount + "(" + pagesToMb(writeCount) + " m) pages flushed");
        }
        this.mLastOldPages = 0;
        checkTime(startTime, "flush pages", 1000L);
        Slog.i(TAG, "After flushing");
        if (LOG_VERBOSE) {
            Slog.w(TAG, "Quota left " + pagesToMb(getQuotaLeft()) + "m");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showTotalStat() {
        Slog.i(TAG, "Total stat:");
        Slog.i(TAG, "  total marked " + this.mTotalMarked + "(" + pagesToMb(this.mTotalMarked) + " m), total flushed " + this.mTotalFlushed + "(" + pagesToMb(this.mTotalFlushed) + " m)");
        try {
            String wbStats = FileUtils.readTextFile(new File(WB_STATS_SYS), 128, "");
            Slog.i(TAG, "  wb stat: " + wbStats);
        } catch (IOException e) {
            Slog.i(TAG, "  wb stat: error");
        }
        try {
            String mm_stats = FileUtils.readTextFile(new File(MM_STATS_SYS), 128, "");
            Slog.i(TAG, "  mm stat: " + mm_stats);
        } catch (IOException e2) {
            Slog.i(TAG, "  mm  stat: error");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean tryToMarkPages() {
        if (getQuotaLeft() <= 0) {
            if (LOG_VERBOSE) {
                Slog.d(TAG, "Total flush pages over limit, exit");
                return false;
            }
            return false;
        }
        long startTime = SystemClock.uptimeMillis();
        Slog.d(TAG, "Start marking ...");
        runMark();
        checkTime(startTime, "mark pages", 100L);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean tryToFlushPages() {
        if (getQuotaLeft() <= 0) {
            if (LOG_VERBOSE) {
                Slog.d(TAG, "Total flush pages over limit, exit");
                return false;
            }
            return false;
        }
        runFlush(FLUSH_LEVEL);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryToStopFlush() {
        stopFlush();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateState(int newState) {
        Slog.i(TAG, "State update : " + this.mCurState + " -> " + newState);
        this.mCurState = newState;
    }

    private boolean isFlushablePagesExist(int flush_level, int flush_count_least) {
        if (flush_level < FLUSH_HIGH_LEVEL || flush_level > FLUSH_LOW_LEVEL) {
            return false;
        }
        try {
            String idlePages = FileUtils.readTextFile(new File(OLD_COUNT_SYS), 0, "");
            if (idlePages == null) {
                return false;
            }
            String[] pages = idlePages.split("\\s+");
            int pagesCount = 0;
            for (int i = flush_level - 1; i < pages.length; i++) {
                pagesCount += Integer.parseInt(pages[i]);
            }
            if (pagesCount < flush_count_least) {
                Slog.i(TAG, "the number of flushable pages is relatively few");
                return false;
            }
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "Failed to get pages need to flush");
            return true;
        }
    }

    private boolean flushIsAllowed() {
        if (this.lastFlushTime != 0 && SystemClock.uptimeMillis() - this.lastFlushTime < FLUSH_INTERVAL_LIMIT) {
            Slog.i(TAG, "runFlush  interval less than FLUSH_INTERVAL_LIMIT, skip flush");
            return false;
        }
        if (FLUSH_ZRAM_USEDRATE > getZramUseRate()) {
            Slog.i(TAG, "zram userate is low, no need to flush: " + getZramUseRate());
            return false;
        }
        return true;
    }

    private boolean reportLowMemPressureLimit(int pressureState) {
        if (pressureState != 1) {
            return true;
        }
        if (System.currentTimeMillis() - this.lastReportMPtime >= 30000) {
            this.lastReportMPtime = System.currentTimeMillis();
            this.reportMemPressureCnt = 0;
        }
        int i = this.reportMemPressureCnt;
        if (i >= 3) {
            return false;
        }
        this.reportMemPressureCnt = i + 1;
        return true;
    }

    private void checkTime(long startTime, String where, long threshold) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > threshold) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerAlarmClock(long starttime) {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (alarmManager != null) {
            Intent intent = new Intent();
            intent.setAction(BROADCASTS_ACTION_MARK);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, BROADCASTS_ACTION_MARK.hashCode(), intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
            alarmManager.setExactAndAllowWhileIdle(0, starttime, pendingIntent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterAlarmClock() {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (alarmManager != null) {
            Intent intent = new Intent();
            intent.setAction(BROADCASTS_ACTION_MARK);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, BROADCASTS_ACTION_MARK.hashCode(), intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }

    public void reportMemPressure(int pressureState) {
        int flush_level;
        int msg_do_flush_level;
        if (!this.isInitSuccess || pressureState < 1 || !reportLowMemPressureLimit(pressureState) || !flushIsAllowed()) {
            return;
        }
        switch (pressureState) {
            case 1:
                flush_level = FLUSH_LOW_LEVEL;
                this.flush_per_quota = PER_FLUSH_QUOTA_LOW;
                msg_do_flush_level = 1;
                break;
            case 2:
                flush_level = FLUSH_MEDIUM_LEVEL;
                this.flush_per_quota = PER_FLUSH_QUOTA_MEDIUM;
                msg_do_flush_level = 2;
                break;
            case 3:
                flush_level = FLUSH_HIGH_LEVEL;
                this.flush_per_quota = PER_FLUSH_QUOTA_HIGH;
                msg_do_flush_level = 3;
                break;
            default:
                return;
        }
        int flush_count_least = this.flush_per_quota / 2;
        if (isFlushablePagesExist(flush_level, flush_count_least) && this.isFlushFinished && getQuotaLeft() > 0) {
            try {
                this.mHandler.removeMessages(4);
                Slog.i(TAG, "reportMemPressure: " + pressureState + ", start flush");
                this.mHandler.sendEmptyMessage(msg_do_flush_level);
                this.mHandler.sendEmptyMessageDelayed(4, DELAY_DURATION);
            } catch (Exception e) {
                Slog.i(TAG, "reportMemPressure error ");
            }
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        if (params.getJobId() != RESET_WB_LIMIT_JOB_ID) {
            return false;
        }
        sExtendMRecord.setDailyRecord(this.mTotalMarked, this.mTotalFlushed);
        this.mTotalMarked = 0L;
        this.mTotalFlushed = 0L;
        setFlushQuota();
        resetNextFlushQuota(this);
        jobFinished(params, false);
        return true;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connect() {
        IBinder service = ServiceManager.getService("vold");
        this.binder = service;
        if (service != null && this.mVold == null) {
            try {
                this.binder.linkToDeath(this.death, 0);
            } catch (RemoteException e) {
                this.binder = null;
            }
        }
        if (this.binder != null && this.mVold == null) {
            Slog.i(TAG, "init vold");
            this.mVold = IVold.Stub.asInterface(this.binder);
        }
    }

    private void disconnect() {
        if (this.binder != null && this.mVold != null) {
            this.binder.unlinkToDeath(this.death, 0);
        }
        this.mVold = null;
        this.binder = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startExtM() {
        if (isKernelSupported()) {
            if (EXTM_SETTINGS_PROP.equals("1")) {
                Handler handler = null;
                ContentObserver observer = new ContentObserver(handler) { // from class: com.android.server.ExtendMImpl.3
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        ExtendMImpl.this.refreshExtmSettings();
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_EXT_MEM_PROP), false, observer, -2);
                parseExtMemCloudControl();
                if (isCloudAllowed()) {
                    init();
                }
                if (isMfzSupported()) {
                    ContentObserver mfzobsever = new ContentObserver(handler) { // from class: com.android.server.ExtendMImpl.4
                        @Override // android.database.ContentObserver
                        public void onChange(boolean selfChange) {
                            ExtendMImpl.this.ParseMfzSettings();
                            ExtendMImpl.this.SetMfzEnable();
                        }
                    };
                    this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MFZ_PROP), false, mfzobsever, -2);
                    ParseMfzSettings();
                    if (!isMfzCloud()) {
                        SetMfzEnable();
                    }
                }
                ContentObserver dmoptobsever = new ContentObserver(handler) { // from class: com.android.server.ExtendMImpl.5
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        ExtendMImpl.this.ParsedmoptSettings();
                        ExtendMImpl.this.SetdmoptEnable();
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_DM_OPT_PROP), false, dmoptobsever, -2);
                return;
            }
            return;
        }
        int i = this.kernelSupportCheckCnt;
        if (i < 30) {
            this.kernelSupportCheckCnt = i + 1;
            this.mHandler.sendEmptyMessageDelayed(5, 1000L);
        } else {
            Slog.d(TAG, "the number of times has reached the upper limit, skip");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean runFlush(int flushLevel) {
        getPagesNeedFlush(flushLevel);
        if (this.flush_number_adjust <= 0) {
            Slog.w(TAG, "no pages need to flush");
            return false;
        }
        if (flushLevel == FLUSH_LOW_LEVEL) {
            Slog.d(TAG_AM, "Report MEM_PRESSURE_LOW!  Flush Level is: " + flushLevel + " Need Flush: " + this.flush_number_adjust + " pages ");
        } else if (flushLevel == FLUSH_MEDIUM_LEVEL) {
            Slog.d(TAG_AM, "Report MEM_PRESSURE_MEDIUM!  Flush Level is: " + flushLevel + " Need Flush: " + this.flush_number_adjust + " pages ");
        } else {
            Slog.d(TAG_AM, "Report MEM_PRESSURE_HIGH!  Flush Level is: " + flushLevel + " Need Flush: " + this.flush_number_adjust + " pages ");
        }
        try {
            String before_idle_stats = FileUtils.readTextFile(new File(OLD_COUNT_SYS), 128, "");
            Slog.d(TAG_AM, "before Flush idle_stats: " + before_idle_stats);
        } catch (IOException e) {
            Slog.d(TAG_AM, "before Flush idle_stats: error");
        }
        this.isFlushFinished = false;
        Slog.w(TAG, "Flush thread start , " + this.flush_number_adjust + " pages need to flush");
        try {
            Method method = IVold.class.getDeclaredMethod("runExtMFlush", Integer.TYPE, Integer.TYPE, IVoldTaskListener.class);
            method.invoke(this.mVold, Integer.valueOf(this.flush_number_adjust), Integer.valueOf(this.flush_level_adjust), new IVoldTaskListener.Stub() { // from class: com.android.server.ExtendMImpl.6
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    Slog.w(ExtendMImpl.TAG, "Flush finished with status: " + status);
                    try {
                        String idle_stats = FileUtils.readTextFile(new File(ExtendMImpl.OLD_COUNT_SYS), 128, "");
                        Slog.d(ExtendMImpl.TAG_AM, "Flush finished! After Flush idle_stats: " + idle_stats);
                    } catch (IOException e2) {
                        Slog.d(ExtendMImpl.TAG_AM, "After Flush idle_stats: error");
                    }
                    try {
                        String bd_stats = FileUtils.readTextFile(new File(ExtendMImpl.WB_STATS_SYS), 128, "");
                        Slog.d(ExtendMImpl.TAG_AM, "Flush finished! After Flush bd_stats: " + bd_stats);
                    } catch (IOException e3) {
                        Slog.d(ExtendMImpl.TAG_AM, "After Flush bd_stats: error");
                    }
                    ExtendMImpl.this.readMemInfo();
                    long zramFree = ((Long) ExtendMImpl.this.mMemInfo.get(ExtendMImpl.MEMINFO_SWAP_FREE)).longValue();
                    Slog.d(ExtendMImpl.TAG_AM, "Flush finished! After Flush zramFreeKb: " + zramFree + "KB");
                    ExtendMImpl.this.isFlushFinished = true;
                    if (status == 0) {
                        ExtendMImpl.this.lastFlushTime = SystemClock.uptimeMillis();
                        ExtendMImpl extendMImpl = ExtendMImpl.this;
                        extendMImpl.showAndSaveFlushedStat(extendMImpl.prev_bd_write, ExtendMImpl.this.lastFlushTime);
                        if (ExtendMImpl.LOG_VERBOSE) {
                            Slog.d(ExtendMImpl.TAG, "Exit flush");
                        }
                        ExtendMImpl.this.showTotalStat();
                        return;
                    }
                    if (status == -1) {
                        Slog.w(ExtendMImpl.TAG, "Flush process fork error ");
                    }
                }
            });
        } catch (Exception e2) {
            Slog.wtf(TAG, e2);
            Slog.e(TAG, "Failed to runExtmFlushPages");
        }
        return true;
    }

    private boolean runMark() {
        Slog.w(TAG, "Mark thread start ");
        try {
            Method method = IVold.class.getDeclaredMethod("runExtMMark", IVoldTaskListener.class);
            method.invoke(this.mVold, new IVoldTaskListener.Stub() { // from class: com.android.server.ExtendMImpl.7
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    Slog.w(ExtendMImpl.TAG, "Mark finished with status: " + status);
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            Slog.e(TAG, "Failed to runExtmMarkPages");
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopFlush() {
        Slog.d(TAG, "-----------------------");
        try {
            Method method = IVold.class.getDeclaredMethod("stopExtMFlush", IVoldTaskListener.class);
            method.invoke(this.mVold, new IVoldTaskListener.Stub() { // from class: com.android.server.ExtendMImpl.8
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    if (status == 0) {
                        Slog.w(ExtendMImpl.TAG, "send SIGUSR1 to flush thread success");
                        ExtendMImpl.this.updateState(3);
                    } else if (status == -1) {
                        Slog.w(ExtendMImpl.TAG, "The flush thread is not running");
                    }
                    ExtendMImpl.this.isFlushFinished = true;
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            Slog.e(TAG, "Failed to stopExtmFlushPages");
        }
    }

    private static Calendar tomorrowMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, 2);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendar.add(5, 1);
        return calendar;
    }

    private void saveFlushData(int writeCount, String mReadBackRate, long startTime) {
        long now = SystemClock.uptimeMillis();
        long costTime = now - startTime;
        sExtendMRecord.setFlushRecord(writeCount, (int) pagesToMb(writeCount), mReadBackRate, costTime);
    }

    public void dump(IndentingPrintWriter pw) {
        try {
            LinkedList<String> records = sExtendMRecord.getSingleRecord();
            LinkedList<String> dailyRecords = sExtendMRecord.getDailyRecords();
            pw.println("ExtM details:");
            pw.increaseIndent();
            Iterator<String> it = records.iterator();
            while (it.hasNext()) {
                String record = it.next();
                pw.println(record);
            }
            pw.decreaseIndent();
            pw.println();
            pw.println("ExtM daily details:");
            pw.increaseIndent();
            Iterator<String> it2 = dailyRecords.iterator();
            while (it2.hasNext()) {
                String dailyRecord = it2.next();
                pw.println(dailyRecord);
            }
            pw.decreaseIndent();
            pw.println();
        } catch (Exception e) {
            pw.println("ExtM not init, nothing to dump");
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
                    ExtendMImpl.this.runFlush(ExtendMImpl.FLUSH_LOW_LEVEL);
                    return;
                case 2:
                    ExtendMImpl.this.runFlush(ExtendMImpl.FLUSH_MEDIUM_LEVEL);
                    return;
                case 3:
                    ExtendMImpl.this.runFlush(ExtendMImpl.FLUSH_HIGH_LEVEL);
                    return;
                case 4:
                    if (!ExtendMImpl.this.isFlushFinished) {
                        Slog.d(ExtendMImpl.TAG, "flush time out , stop it");
                        ExtendMImpl.this.stopFlush();
                        return;
                    }
                    return;
                case 5:
                    ExtendMImpl.this.startExtM();
                    return;
                case 6:
                    ExtendMImpl.this.tryToMarkPages();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class ExtendMRecord {
        private static final String FILE_PATH = "/data/mqsas/extm/dailyrecord.txt";
        private String mDailyRecordData;
        private String mRecordData;
        private String mTimeStamp;
        private int MAX_RECORD_COUNT = MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
        private int MAX_DAILY_RECORD_COUNT = 7;
        private int mMarkCount = 0;
        private int mFlushCount = 0;
        private double mReadBackRateTotal = 0.0d;
        public LinkedList<String> records = new LinkedList<>();
        public LinkedList<String> dailyRecords = new LinkedList<>();

        ExtendMRecord() {
        }

        public LinkedList<String> getSingleRecord() {
            return this.records;
        }

        public LinkedList<String> getDailyRecords() {
            return this.dailyRecords;
        }

        public void setFlushRecord(int writeCount, int writeTotal, String readBackRate, long costTime) {
            this.mFlushCount++;
            this.mReadBackRateTotal += Double.parseDouble(readBackRate);
            String recordData = "Flushed finished: " + writeCount + "(" + writeTotal + " m) flushed, and read back rate is " + readBackRate + " , flush costs " + costTime + " ms";
            this.mTimeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String str = this.mTimeStamp + ": " + recordData;
            this.mRecordData = str;
            updateSingleRecord(str);
        }

        public void setDailyRecord(long totalMarked, long totalFlushed) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(5, -1);
            Date date = calendar.getTime();
            int avgRBRate = (int) (this.mReadBackRateTotal / this.mFlushCount);
            JSONObject obj = null;
            try {
                obj = new JSONObject();
                obj.put("date", date);
                obj.put("markCount", this.mMarkCount);
                obj.put("markTotal", totalMarked);
                obj.put("flushCount", this.mFlushCount);
                obj.put("flushTotal", totalFlushed);
                obj.put("readBackRate", avgRBRate);
            } catch (JSONException ignored) {
                ignored.printStackTrace();
            }
            if (obj == null) {
                return;
            }
            this.mDailyRecordData = obj.toString();
            writeToFile(this.mDailyRecordData + "\n", FILE_PATH);
            if (ExtendMImpl.LOG_VERBOSE) {
                Slog.d(ExtendMImpl.TAG, "daily record data : " + this.mDailyRecordData);
            }
            updateDailyRecord(this.mDailyRecordData);
        }

        public void updateSingleRecord(String recordData) {
            if (this.records.size() < this.MAX_RECORD_COUNT) {
                this.records.addLast(recordData);
            } else {
                this.records.removeFirst();
                this.records.addLast(recordData);
            }
        }

        public void updateDailyRecord(String dailyRecord) {
            if (this.dailyRecords.size() < this.MAX_DAILY_RECORD_COUNT) {
                this.dailyRecords.addLast(dailyRecord);
            } else {
                this.dailyRecords.removeFirst();
                this.dailyRecords.addLast(dailyRecord);
            }
            this.mMarkCount = 0;
            this.mFlushCount = 0;
            this.mReadBackRateTotal = 0.0d;
        }

        public void writeToFile(String line, String path) {
            Slog.w(ExtendMImpl.TAG, " write daily extm usage to /data/mqsas/extm/dailyrecord.txt");
            FileOutputStream fos = null;
            File file = new File(path);
            if (!file.exists()) {
                try {
                    file.getParentFile().mkdir();
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                try {
                    try {
                        fos = new FileOutputStream(file, true);
                        byte[] bytes = line.getBytes();
                        fos.write(bytes);
                        fos.close();
                        fos.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        if (fos != null) {
                            fos.close();
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } catch (Throwable th) {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception e22) {
                        e22.printStackTrace();
                    }
                }
                throw th;
            }
        }
    }
}
