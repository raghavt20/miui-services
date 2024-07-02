package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.am.MemoryFreezeStubImpl;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class MemoryFreezeStubImpl implements MemoryFreezeStub {
    private static final String CLOUD_MEMFREEZE_ENABLE = "cloud_memFreeze_control";
    private static final long DEFAULT_FREEZER_DEBOUNCE_TIMEOUT = 120000;
    private static final String EXTM_ENABLE_PROP = "persist.miui.extm.enable";
    private static final int FLUSH_STAT_INDEX = 0;
    private static final String FLUSH_STAT_PATH = "/sys/block/zram0/mfz_compr_data_size";
    private static final String MEMFREEZE_ENABLE_PROP = "persist.sys.mfz.enable";
    private static final String MEMFREEZE_POLICY_PROP = "persist.sys.mfz.mem_policy";
    private static final int MSG_CHECK_KILL = 6;
    private static final int MSG_CHECK_RECLAIM = 4;
    private static final int MSG_CHECK_UNUSED = 3;
    private static final int MSG_CHECK_WRITE_BACK = 5;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 1;
    private static final int MSG_REGISTER_MIUIFREEZE_OBSERVER = 2;
    private static final int MSG_REGISTER_WHITE_LIST_OBSERVER = 7;
    private static final String PROCESS_FREEZE_LIST = "miui_freeze";
    private static final String TAG = "MFZ";
    private static final String WHITELIST_CLOUD_PATH = "/data/system/mfz.xml";
    private static final String WHITELIST_DEFAULT_PATH = "/product/etc/mfz.xml";
    private volatile boolean extmEnable;
    private boolean isInit;
    private ActivityManagerService mAMS;
    private ActivityTaskManagerService mATMS;
    private BinderService mBinderService;
    private Context mContext;
    private volatile boolean mEnable;
    private volatile long mFreezerDebounceTimeout;
    public MyHandler mHandler;
    private HandlerThread mHandlerThread;
    private int mKillingUID;
    private String mLastStartPackage;
    private long mLastStartPackageTime;
    private MemoryFreezeCloud mMemoryFreezeCloud;
    private ProcessManagerService mPMS;
    private List<Integer> mProcessFrozenUid;
    private HashMap<Integer, RamFrozenAppInfo> mRunningPackages;
    private long mUnusedCheckTime;
    private long mUnusedKillTime;
    private WindowManagerInternal mWMInternal;
    private IStorageManager storageManager;
    private static long sThreshholdReclaim = 2097152;
    private static long sThreshholdWriteback = 3145728;
    private static long sThreshholdKill = 4194304;
    private static final String MEMFREEZE_DEBUG_PROP = "persist.miui.extm.debug_enable";
    private static boolean DEBUG = SystemProperties.getBoolean(MEMFREEZE_DEBUG_PROP, false);
    private static List<String> sWhiteListApp = new ArrayList();
    private static List<String> sBlackListApp = new ArrayList();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MemoryFreezeStubImpl> {

        /* compiled from: MemoryFreezeStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MemoryFreezeStubImpl INSTANCE = new MemoryFreezeStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MemoryFreezeStubImpl m488provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MemoryFreezeStubImpl m487provideNewInstance() {
            return new MemoryFreezeStubImpl();
        }
    }

    public MemoryFreezeStubImpl() {
        this.extmEnable = SystemProperties.getInt(EXTM_ENABLE_PROP, 0) == 1;
        this.mEnable = SystemProperties.getBoolean(MEMFREEZE_ENABLE_PROP, false);
        this.mFreezerDebounceTimeout = DEFAULT_FREEZER_DEBOUNCE_TIMEOUT;
        this.mAMS = null;
        this.mWMInternal = null;
        this.mATMS = null;
        this.mPMS = null;
        this.storageManager = null;
        this.mBinderService = null;
        this.isInit = false;
        this.mContext = null;
        this.mRunningPackages = new HashMap<>();
        this.mHandlerThread = new HandlerThread(TAG);
        this.mProcessFrozenUid = new ArrayList();
        this.mHandler = null;
        this.mMemoryFreezeCloud = null;
        this.mLastStartPackage = null;
        this.mLastStartPackageTime = 0L;
        this.mUnusedCheckTime = 604800000L;
        this.mUnusedKillTime = 2419200000L;
        this.mKillingUID = -1;
    }

    public boolean isEnable() {
        return this.extmEnable && this.mEnable && this.isInit;
    }

    public static MemoryFreezeStubImpl getInstance() {
        return (MemoryFreezeStubImpl) MiuiStubUtil.getImpl(MemoryFreezeStub.class);
    }

    public void init(Context context, ActivityManagerService ams) {
        boolean isCnVersion = "cn".equals(SystemProperties.get("ro.miui.build.region", ""));
        if (isCnVersion && !this.isInit && this.extmEnable && this.mEnable) {
            this.mContext = context;
            MemoryFreezeCloud memoryFreezeCloud = new MemoryFreezeCloud();
            this.mMemoryFreezeCloud = memoryFreezeCloud;
            memoryFreezeCloud.initCloud(context);
            this.mHandlerThread.start();
            this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
            getWhitePackages();
            updateThreshhold();
            Message msgRegisterCloud = this.mHandler.obtainMessage(1);
            this.mHandler.sendMessage(msgRegisterCloud);
            Message msgCheckUnused = this.mHandler.obtainMessage(3);
            this.mHandler.sendMessageDelayed(msgCheckUnused, this.mUnusedCheckTime);
            Message msgRegisterWhiteList = this.mHandler.obtainMessage(7);
            this.mHandler.sendMessage(msgRegisterWhiteList);
            BinderService binderService = new BinderService();
            this.mBinderService = binderService;
            ServiceManager.addService("memfreeze", binderService);
            this.mAMS = ams;
            this.mWMInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mATMS = ams.mActivityTaskManager;
            this.mPMS = (ProcessManagerService) ServiceManager.getService("ProcessManager");
            this.storageManager = getStorageManager();
            this.isInit = true;
            if (DEBUG) {
                Slog.d(TAG, "complete init, default applist: " + sWhiteListApp);
            }
        }
    }

    private static IStorageManager getStorageManager() {
        try {
            return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        } catch (VerifyError e) {
            Slog.e(TAG, "get StorageManager error");
            return null;
        }
    }

    private void updateThreshhold() {
        String prop = SystemProperties.get(MEMFREEZE_POLICY_PROP, "2048,3072,4096");
        try {
            String[] res = prop.split(",");
            if (res.length == 3) {
                sThreshholdReclaim = Long.valueOf(res[0]).longValue() * FormatBytesUtil.KB;
                sThreshholdWriteback = Long.valueOf(res[1]).longValue() * FormatBytesUtil.KB;
                sThreshholdKill = Long.valueOf(res[2]).longValue() * FormatBytesUtil.KB;
            }
        } catch (Exception e) {
            sThreshholdReclaim = 2097152L;
            sThreshholdWriteback = 3145728L;
            sThreshholdKill = 4194304L;
            e.printStackTrace();
        }
        Slog.i(TAG, "mfz policy: " + sThreshholdReclaim + "," + sThreshholdWriteback + "," + sThreshholdKill);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:9:0x0030. Please report as an issue. */
    public void getWhitePackages() {
        String path = WHITELIST_CLOUD_PATH;
        if (!new File(WHITELIST_CLOUD_PATH).exists()) {
            path = WHITELIST_DEFAULT_PATH;
            Slog.i(TAG, "looking for default xml.");
        }
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(path);
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStream, "utf-8");
                for (int event = xmlParser.getEventType(); event != 1; event = xmlParser.next()) {
                    switch (event) {
                        case 0:
                        case 1:
                        default:
                        case 2:
                            if ("designated-package".equals(xmlParser.getName())) {
                                sWhiteListApp.add(xmlParser.getAttributeValue(0));
                            }
                        case 3:
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, e.getMessage());
                try {
                    if (inputStream == null) {
                        return;
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, e2.getMessage());
                    }
                } finally {
                }
            }
            try {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    Slog.e(TAG, e3.getMessage());
                }
            } finally {
            }
        } catch (Throwable th) {
            try {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                        Slog.e(TAG, e4.getMessage());
                    }
                }
                throw th;
            } finally {
            }
        }
    }

    public void updateCloudControlParas() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_ENABLE, -2) != null) {
            String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_ENABLE, -2);
            this.extmEnable = !enableStr.equals("0");
            Slog.w(TAG, "set enable state from database: " + this.extmEnable);
        }
    }

    public void updateMiuiFreezeInfo() {
        if (isEnable()) {
            String miuiFreezeStr = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), PROCESS_FREEZE_LIST, -2);
            if (DEBUG) {
                Slog.d(TAG, "SettingsContentObserver change str=" + miuiFreezeStr);
            }
            if (miuiFreezeStr != null) {
                List<Integer> thawList = new ArrayList<>();
                List<Integer> freezeList = new ArrayList<>();
                String freezeSub = "";
                String thawSub = "";
                if (miuiFreezeStr.contains("thaw")) {
                    thawSub = miuiFreezeStr.split("thaw:")[1].split(";freeze:")[0];
                    if (miuiFreezeStr.contains("freeze")) {
                        freezeSub = miuiFreezeStr.split("thaw:")[1].split(";freeze:")[1];
                    }
                } else if (miuiFreezeStr.contains("freeze")) {
                    freezeSub = miuiFreezeStr.split(";freeze:")[1];
                }
                if (thawSub.length() > 0) {
                    for (String str : thawSub.split(", ")) {
                        thawList.add(Integer.valueOf(Integer.parseInt(str)));
                    }
                }
                if (freezeSub.length() > 0) {
                    String[] tmpStr = freezeSub.split(", ");
                    for (String str2 : tmpStr) {
                        freezeList.add(Integer.valueOf(Integer.parseInt(str2)));
                    }
                }
                updateFrozedUids(freezeList, thawList);
            }
        }
    }

    public void registerMiuiFreezeObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.MemoryFreezeStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.Secure.getUriFor(MemoryFreezeStubImpl.PROCESS_FREEZE_LIST))) {
                    MemoryFreezeStubImpl.this.updateMiuiFreezeInfo();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(PROCESS_FREEZE_LIST), false, observer, -2);
    }

    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.MemoryFreezeStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryFreezeStubImpl.CLOUD_MEMFREEZE_ENABLE))) {
                    MemoryFreezeStubImpl.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMFREEZE_ENABLE), false, observer, -2);
    }

    public boolean isMemoryFreezeWhiteList(String packageName) {
        return isEnable() && sWhiteListApp.contains(packageName);
    }

    public void activityResumed(int uid, String packageName, String processName) {
        if (isEnable() && sWhiteListApp.contains(packageName)) {
            long now = SystemClock.uptimeMillis();
            if (packageName.equals(this.mLastStartPackage) && now - this.mLastStartPackageTime <= 5000) {
                return;
            }
            this.mLastStartPackageTime = now;
            this.mLastStartPackage = packageName;
            synchronized (this.mRunningPackages) {
                RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
                if (target == null) {
                    this.mRunningPackages.put(Integer.valueOf(uid), new RamFrozenAppInfo(uid, packageName));
                } else {
                    this.mHandler.removeEqualMessages(4, Integer.valueOf(uid));
                    target.mPendingFreeze = false;
                    target.mFrozen = false;
                }
            }
            Slog.d(TAG, "activityResumed packageName:" + packageName + "|" + processName + ",uid:" + uid);
        }
    }

    public void activityStopped(int uid, int pid, String packageName) {
        if (isEnable() && sWhiteListApp.contains(packageName)) {
            synchronized (this.mRunningPackages) {
                RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
                if (target != null) {
                    target.mPendingFreeze = true;
                    target.mlastActivityStopTime = System.currentTimeMillis();
                    target.mPid = pid;
                }
            }
            this.mHandler.removeEqualMessages(4, Integer.valueOf(uid));
            Message msg = this.mHandler.obtainMessage(4, Integer.valueOf(uid));
            this.mHandler.sendMessageDelayed(msg, this.mFreezerDebounceTimeout);
            Slog.d(TAG, "activityStopped packageName:" + packageName + ",uid:" + uid + ",pid:" + pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkUnused() {
        if (this.mPMS == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        final List<Integer> killList = new ArrayList<>();
        synchronized (this.mRunningPackages) {
            this.mRunningPackages.forEach(new BiConsumer() { // from class: com.android.server.am.MemoryFreezeStubImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    MemoryFreezeStubImpl.this.lambda$checkUnused$0(now, killList, (Integer) obj, (MemoryFreezeStubImpl.RamFrozenAppInfo) obj2);
                }
            });
        }
        Iterator<Integer> it = killList.iterator();
        while (it.hasNext()) {
            int uid = it.next().intValue();
            for (ProcessRecord app : this.mPMS.getProcessRecordByUid(uid)) {
                if (app != null && app.mState.getCurAdj() >= 700) {
                    this.mPMS.getProcessKiller().killApplication(app, "mfz-overtime", false);
                    Slog.d(TAG, "kill " + app.processName + "(" + app.mPid + ") since overtime.");
                }
            }
        }
        MyHandler myHandler = this.mHandler;
        myHandler.sendMessageDelayed(myHandler.obtainMessage(3), this.mUnusedCheckTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkUnused$0(long now, List killList, Integer key, RamFrozenAppInfo value) {
        if (value.mlastActivityStopTime > 0 && now - value.mlastActivityStopTime > this.mUnusedKillTime) {
            killList.add(Integer.valueOf(value.mUid));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndReclaim(int uid) {
        ProcessRecord app;
        if (countBgMem() < sThreshholdReclaim) {
            Slog.i(TAG, "don't need to PR " + uid);
            return;
        }
        synchronized (this.mRunningPackages) {
            RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
            if (target == null) {
                return;
            }
            target.mPendingFreeze = false;
            target.mFrozen = true;
            String pkgName = target.mPackageName;
            List<Integer> visibleUids = this.mWMInternal.getVisibleWindowOwner();
            if (visibleUids.indexOf(Integer.valueOf(uid)) != -1) {
                Slog.w(TAG, "don't PR " + pkgName + " as visible.");
                return;
            }
            synchronized (this.mAMS) {
                app = this.mAMS.getProcessRecordLocked(pkgName, uid);
            }
            if (app == null) {
                Slog.e(TAG, "didn't find process record of " + pkgName + "(" + uid + ")");
            } else {
                Slog.d(TAG, "PR " + pkgName + ",uid:" + uid + ",pid:" + app.mPid);
                this.mAMS.mOomAdjuster.mCachedAppOptimizer.compactMemoryFreezeApp(app, true, new Runnable() { // from class: com.android.server.am.MemoryFreezeStubImpl.3
                    @Override // java.lang.Runnable
                    public void run() {
                        MemoryFreezeStubImpl.this.processReclaimFinished();
                    }
                });
            }
        }
    }

    public void processReclaimFinished() {
        if (this.mHandler.hasMessages(6)) {
            Slog.d(TAG, "remove kill msg");
            this.mHandler.removeMessages(6);
        }
        if (this.mHandler.hasMessages(5)) {
            Slog.d(TAG, "remove WB msg");
            this.mHandler.removeMessages(5);
        }
        Message msg = this.mHandler.obtainMessage(5);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndWriteBack() {
        if (countBgMem() < sThreshholdWriteback) {
            Slog.i(TAG, "don't need to WB.");
            return;
        }
        if (this.storageManager == null) {
            return;
        }
        try {
            Slog.d(TAG, "WB in progress");
            this.storageManager.runExtmFlushPages(2, new IVoldTaskListener.Stub() { // from class: com.android.server.am.MemoryFreezeStubImpl.4
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    Slog.d(MemoryFreezeStubImpl.TAG, "WB finished, count " + extras.getInt("MFZ_PAGE_COUNT"));
                    MemoryFreezeStubImpl.this.extmFlushFinished();
                }
            });
        } catch (Exception e) {
            Slog.e(TAG, "WB error");
            e.printStackTrace();
        }
    }

    public void extmFlushFinished() {
        if (this.mHandler.hasMessages(6)) {
            Slog.d(TAG, "remove kill msg");
            this.mHandler.removeMessages(6);
        }
        Message msg = this.mHandler.obtainMessage(6);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndKill() {
        if (countBgMem() < sThreshholdKill) {
            Slog.i(TAG, "don't need to kill.");
            return;
        }
        int needKillUid = -1;
        long needKillLastStopTime = System.currentTimeMillis() - this.mFreezerDebounceTimeout;
        synchronized (this.mRunningPackages) {
            Iterator<Integer> it = this.mRunningPackages.keySet().iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
                if (target.mlastActivityStopTime > 0 && target.mlastActivityStopTime < needKillLastStopTime) {
                    needKillUid = uid;
                    needKillLastStopTime = target.mlastActivityStopTime;
                }
            }
        }
        if (needKillUid > 0) {
            for (ProcessRecord app : this.mPMS.getProcessRecordByUid(needKillUid)) {
                if (app != null && app.mState.getCurAdj() >= 700) {
                    this.mPMS.getProcessKiller().killApplication(app, "mfz-overmem", false);
                    Slog.d(TAG, "Killing " + app.processName + "(" + app.mPid + ") since overmem.");
                }
            }
            this.mKillingUID = needKillUid;
            return;
        }
        this.mKillingUID = -1;
        Slog.w(TAG, "Stop checking and killing as no apps to kill.");
    }

    private long countBgMem() {
        long pssTotal = 0;
        List<Integer> visibleUids = this.mWMInternal.getVisibleWindowOwner();
        synchronized (this.mRunningPackages) {
            Iterator<Integer> it = this.mRunningPackages.keySet().iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
                if (target.mPid > 0 && visibleUids.indexOf(Integer.valueOf(uid)) == -1) {
                    long pss = Process.getPss(target.mPid) / FormatBytesUtil.KB;
                    if (DEBUG) {
                        Slog.d(TAG, "pss: " + pss + " " + target.mPackageName + ",uid:" + uid + ",pid:" + target.mPid);
                    }
                    pssTotal += pss;
                }
            }
        }
        long flushTotal = countBgFlush();
        long countBgMem = pssTotal + flushTotal;
        Slog.d(TAG, "mem count: " + pssTotal + " + " + flushTotal + " = " + countBgMem);
        return countBgMem;
    }

    private long countBgFlush() {
        long flushCount = 0;
        File file = new File(FLUSH_STAT_PATH);
        BufferedReader reader = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    String line = reader.readLine();
                    if (line != null) {
                        String[] res = line.trim().split(" +");
                        if (res.length >= 1 && res[0] != null) {
                            flushCount = Long.valueOf(res[0]).longValue() / FormatBytesUtil.KB;
                        }
                    }
                    reader.close();
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        return flushCount;
    }

    public boolean isNeedProtect(ProcessRecord app) {
        if (!isEnable() || app == null) {
            return false;
        }
        String packageName = app.info.packageName;
        return sWhiteListApp.contains(packageName) && !sBlackListApp.contains(packageName);
    }

    public void reportAppDied(ProcessRecord app) {
        if (isEnable() && app != null) {
            String processName = app.processName;
            int uid = app.uid;
            int pid = app.mPid;
            if (processName != null && sWhiteListApp.contains(processName)) {
                this.mHandler.removeEqualMessages(4, Integer.valueOf(uid));
                synchronized (this.mRunningPackages) {
                    RamFrozenAppInfo target = this.mRunningPackages.get(Integer.valueOf(uid));
                    if (target != null) {
                        this.mRunningPackages.remove(Integer.valueOf(uid));
                    }
                }
                Slog.d(TAG, "report " + processName + " is died, uid:" + uid + ",pid:" + pid);
                if (uid == this.mKillingUID && !this.mHandler.hasMessages(6)) {
                    Message msg = this.mHandler.obtainMessage(6);
                    this.mHandler.sendMessage(msg);
                }
            }
        }
    }

    public void updateFrozedUids(List<Integer> freezelist, List<Integer> thawlist) {
        if (isEnable()) {
            String mUpdateFrozenInfo = "update process freeze list:";
            if (!freezelist.isEmpty()) {
                int size = freezelist.size();
                synchronized (this.mProcessFrozenUid) {
                    for (int i = 0; i < size; i++) {
                        int value = freezelist.get(i).intValue();
                        if (!this.mProcessFrozenUid.contains(Integer.valueOf(value))) {
                            this.mProcessFrozenUid.add(Integer.valueOf(value));
                        }
                    }
                }
            }
            if (!thawlist.isEmpty()) {
                int size2 = thawlist.size();
                synchronized (this.mProcessFrozenUid) {
                    for (int i2 = 0; i2 < size2; i2++) {
                        int value2 = thawlist.get(i2).intValue();
                        if (this.mProcessFrozenUid.contains(Integer.valueOf(value2))) {
                            this.mProcessFrozenUid.remove(new Integer(value2));
                        }
                    }
                }
            }
            synchronized (this.mProcessFrozenUid) {
                int size3 = this.mProcessFrozenUid.size();
                if (size3 <= 0) {
                    mUpdateFrozenInfo = "update process freeze list: null";
                } else {
                    for (int i3 = 0; i3 < size3; i3++) {
                        int value3 = this.mProcessFrozenUid.get(i3).intValue();
                        String subStr = " " + String.valueOf(value3);
                        String[] packages = this.mContext.getPackageManager().getPackagesForUid(value3);
                        if (packages != null) {
                            for (String str : packages) {
                                subStr = subStr + "|" + str;
                            }
                            mUpdateFrozenInfo = mUpdateFrozenInfo + subStr;
                        }
                    }
                }
            }
            if (DEBUG) {
                Slog.d(TAG, mUpdateFrozenInfo);
            }
        }
    }

    /* loaded from: classes.dex */
    public class RamFrozenAppInfo {
        private String mPackageName;
        private int mUid;
        private boolean mPendingFreeze = false;
        private boolean mFrozen = false;
        private long mlastActivityStopTime = -1;
        private int mPid = -1;

        public RamFrozenAppInfo(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dump(PrintWriter pw) {
        pw.println("---- Start of MEMFREEZE ----");
        pw.println("App State:");
        synchronized (this.mRunningPackages) {
            for (Map.Entry<Integer, RamFrozenAppInfo> entry : this.mRunningPackages.entrySet()) {
                RamFrozenAppInfo tmpAppInfo = entry.getValue();
                pw.print(entry.getKey());
                pw.print(" " + String.format("%-25s", tmpAppInfo.mPackageName));
                pw.print(" Frozen=" + String.format("%-5s", Boolean.valueOf(tmpAppInfo.mFrozen)));
                pw.print(" Pending=" + String.format("%-5s", Boolean.valueOf(tmpAppInfo.mPendingFreeze)));
            }
        }
        synchronized (this.mProcessFrozenUid) {
            pw.println("Process Frozen:");
            pw.print("  Current Frozen Uid:");
            int size = this.mProcessFrozenUid.size();
            for (int i = 0; i < size; i++) {
                pw.print(" " + this.mProcessFrozenUid.get(i));
            }
        }
        pw.print("\n  Total(Y/N) : ");
        pw.println("\n---- End of MEMFREEZE ----");
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(MemoryFreezeStubImpl.this.mContext, MemoryFreezeStubImpl.TAG, pw)) {
                MemoryFreezeStubImpl.this.dump(pw);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            MemoryFreezeStubImpl memoryFreezeStubImpl = MemoryFreezeStubImpl.this;
            new MemFreezeShellCmd(memoryFreezeStubImpl).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private class MemFreezeShellCmd extends ShellCommand {
        MemoryFreezeStubImpl mMemoryFreezeStubImpl;

        public MemFreezeShellCmd(MemoryFreezeStubImpl memoryFreezeStubImpl) {
            this.mMemoryFreezeStubImpl = memoryFreezeStubImpl;
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
                            c = 1;
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
                    default:
                        c = 65535;
                        break;
                }
            } catch (Exception e) {
                pw.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e(MemoryFreezeStubImpl.TAG, "Error running shell command", e);
            }
            switch (c) {
                case 0:
                    this.mMemoryFreezeStubImpl.dump(pw);
                    return 0;
                case 1:
                    boolean enable = Boolean.parseBoolean(getNextArgRequired());
                    MemoryFreezeStubImpl.this.mEnable = enable;
                    pw.println("memory freeze enabled: " + enable);
                    return 0;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Memory Freeze commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("");
            pw.println("  dump");
            pw.println("    Print current app memory freeze info.");
            pw.println("");
            pw.println("  enable [true|false]");
            pw.println("    Enable/Disable memory freeze.");
            pw.println("");
            pw.println("  debug [true|false]");
            pw.println("    Enable/Disable debug config.");
        }
    }

    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MemoryFreezeStubImpl memoryFreezeStubImpl = MemoryFreezeStubImpl.this;
                    memoryFreezeStubImpl.registerCloudObserver(memoryFreezeStubImpl.mContext);
                    MemoryFreezeStubImpl.this.updateCloudControlParas();
                    return;
                case 2:
                    MemoryFreezeStubImpl memoryFreezeStubImpl2 = MemoryFreezeStubImpl.this;
                    memoryFreezeStubImpl2.registerMiuiFreezeObserver(memoryFreezeStubImpl2.mContext);
                    return;
                case 3:
                    MemoryFreezeStubImpl.this.checkUnused();
                    return;
                case 4:
                    MemoryFreezeStubImpl.this.checkAndReclaim(((Integer) msg.obj).intValue());
                    return;
                case 5:
                    MemoryFreezeStubImpl.this.checkAndWriteBack();
                    return;
                case 6:
                    MemoryFreezeStubImpl.this.checkAndKill();
                    return;
                case 7:
                    MemoryFreezeStubImpl.this.mMemoryFreezeCloud.registerCloudWhiteList(MemoryFreezeStubImpl.this.mContext);
                    MemoryFreezeStubImpl.this.mMemoryFreezeCloud.registerMemfreezeOperation(MemoryFreezeStubImpl.this.mContext);
                    return;
                default:
                    return;
            }
        }
    }
}
