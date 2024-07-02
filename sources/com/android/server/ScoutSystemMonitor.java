package com.android.server;

import android.app.AppScoutStateMachine;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.perfdebug.MessageMonitor;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.os.anr.AnrLatencyTracker;
import com.android.server.ScoutHelper;
import com.android.server.ScoutWatchdogInfo;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppProfilerStub;
import com.android.server.am.ProcessUtils;
import com.android.server.am.StackTracesDumpHelper;
import com.android.server.am.SystemPressureControllerStub;
import com.android.server.am.ThermalTempListener;
import com.android.server.wm.SurfaceAnimationThread;
import com.miui.app.MiuiFboServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import com.miui.server.stability.ScoutLibraryTestManager;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.RebootNullEvent;
import miui.mqsas.sdk.event.SysScoutEvent;

/* loaded from: classes.dex */
public class ScoutSystemMonitor extends ScoutStub {
    public static final int SCOUT_COMPLETED = 0;
    public static final int SCOUT_FW_LEVEL_NORMAL = 0;
    public static final int SCOUT_MEM_CHECK_MSG = 0;
    public static final int SCOUT_MEM_CRITICAL_MSG = 2;
    public static final int SCOUT_MEM_DUMP_MSG = 1;
    public static final int SCOUT_OVERDUE = 3;
    public static final long SCOUT_SYSTEM_IO_TIMEOUT = 30000;
    public static final long SCOUT_SYSTEM_TIMEOUT = 10000;
    public static final int SCOUT_WAITED_HALF = 2;
    public static final int SCOUT_WAITING = 1;
    private static final String TAG = "ScoutSystemMonitor";
    public static final ArrayList<ScoutHandlerChecker> mScoutHandlerCheckers = new ArrayList<>();
    private Context mContext;
    private ScoutHandlerChecker mScoutBinderMonitorChecker;
    private ScoutHandlerChecker mScoutMonitorChecker;
    private Object mScoutSysLock;
    private ActivityManagerService mService;
    private volatile SystemWorkerHandler mSysWorkerHandler;
    private MiuiFboServiceInternal miuiFboService;
    private AppScoutStateMachine mUiScoutStateMachine = null;
    private int scoutLevel = 0;
    private int preScoutLevel = 0;
    private HandlerThread mSysMonitorThread = new HandlerThread(TAG);
    private HandlerThread mSysWorkThread = new HandlerThread("ScoutSystemWork");
    private HandlerThread mSysServiceMonitorThread = new HandlerThread("ScoutSystemServiceMonitor");

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ScoutSystemMonitor> {

        /* compiled from: ScoutSystemMonitor$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ScoutSystemMonitor INSTANCE = new ScoutSystemMonitor();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ScoutSystemMonitor m272provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ScoutSystemMonitor m271provideNewInstance() {
            return new ScoutSystemMonitor();
        }
    }

    public static ScoutSystemMonitor getInstance() {
        return (ScoutSystemMonitor) ScoutStub.getInstance();
    }

    private static void initNativeScout() {
        try {
            Slog.i(TAG, "Load libscout");
            System.loadLibrary(ScoutHelper.FILE_DIR_SCOUT);
        } catch (UnsatisfiedLinkError e) {
            Slog.w(TAG, "can't loadLibrary libscout", e);
        }
    }

    private static void registerThermalTempListener() {
        SystemPressureControllerStub.getInstance().registerThermalTempListener(new ThermalTempListener() { // from class: com.android.server.ScoutSystemMonitor$$ExternalSyntheticLambda0
            public final void onThermalTempChange(int i) {
                ScoutSystemMonitor.lambda$registerThermalTempListener$1(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$registerThermalTempListener$1(final int temp) {
        final long timestamp = SystemClock.elapsedRealtime();
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.ScoutSystemMonitor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MQSEventManagerDelegate.getInstance().reportThermalTempChange(temp, timestamp);
            }
        });
    }

    public ScoutSystemMonitor() {
        initNativeScout();
        registerThermalTempListener();
    }

    public void init(Object mLock) {
        this.mScoutSysLock = mLock;
        HandlerThread handlerThread = this.mSysWorkThread;
        if (handlerThread != null) {
            handlerThread.start();
            this.mSysWorkerHandler = new SystemWorkerHandler(this.mSysWorkThread.getLooper());
        }
        HandlerThread handlerThread2 = this.mSysMonitorThread;
        if (handlerThread2 != null) {
            handlerThread2.start();
            ScoutHandlerChecker scoutHandlerChecker = new ScoutHandlerChecker(this.mSysMonitorThread.getThreadHandler(), "ScoutSystemMonitor thread", 10000L);
            this.mScoutBinderMonitorChecker = scoutHandlerChecker;
            mScoutHandlerCheckers.add(scoutHandlerChecker);
        }
        HandlerThread handlerThread3 = this.mSysServiceMonitorThread;
        if (handlerThread3 != null) {
            handlerThread3.start();
            this.mScoutMonitorChecker = new ScoutHandlerChecker(this.mSysServiceMonitorThread.getThreadHandler(), "ScoutSystemServiceMonitor thread", 10000L);
        }
        ArrayList<ScoutHandlerChecker> arrayList = mScoutHandlerCheckers;
        arrayList.add(this.mScoutMonitorChecker);
        arrayList.add(new ScoutHandlerChecker(new Handler(Looper.getMainLooper()), "main thread", 10000L));
        arrayList.add(new ScoutHandlerChecker(UiThread.getHandler(), "ui thread", 10000L));
        arrayList.add(new ScoutHandlerChecker(IoThread.getHandler(), "i/o thread", 30000L));
        arrayList.add(new ScoutHandlerChecker(DisplayThread.getHandler(), "display thread", 10000L));
        arrayList.add(new ScoutHandlerChecker(AnimationThread.getHandler(), "animation thread", 10000L));
        arrayList.add(new ScoutHandlerChecker(SurfaceAnimationThread.getHandler(), "surface animation thread", 10000L));
        updateScreenState(true);
        ScoutHelper.copyRamoopsFileToMqs();
        ScoutLibraryTestManager.getInstance().init();
    }

    public void crashIfHasDThread(boolean mHasDThread) {
        if (mHasDThread && ScoutHelper.isEnabelPanicDThread(TAG)) {
            Slog.e(TAG, "trigger kernel crash: Has D state thread");
            SystemClock.sleep(3000L);
            ScoutHelper.doSysRqInterface('c');
        }
    }

    public void scoutSystemCheckBinderCallChain(ArrayList<Integer> pids, ArrayList<Integer> nativePids, ScoutWatchdogInfo watchdoginfo) {
        ArrayList<Integer> scoutJavaPids = new ArrayList<>(5);
        ArrayList<Integer> scoutNativePids = new ArrayList<>(5);
        ScoutHelper.ScoutBinderInfo scoutBinderInfo = new ScoutHelper.ScoutBinderInfo(Process.myPid(), 0, "MIUIScout Watchdog");
        scoutJavaPids.add(Integer.valueOf(Process.myPid()));
        ScoutHelper.checkBinderCallPidList(Process.myPid(), scoutBinderInfo, scoutJavaPids, scoutNativePids);
        watchdoginfo.setBinderTransInfo(scoutBinderInfo.getBinderTransInfo() + "\n" + scoutBinderInfo.getProcInfo());
        watchdoginfo.setDThreadState(scoutBinderInfo.getDThreadState());
        watchdoginfo.setMonkeyPid(scoutBinderInfo.getMonkeyPid());
        if (scoutJavaPids.size() > 0) {
            Iterator<Integer> it = scoutJavaPids.iterator();
            while (it.hasNext()) {
                int javaPid = it.next().intValue();
                if (!pids.contains(Integer.valueOf(javaPid))) {
                    pids.add(Integer.valueOf(javaPid));
                    Slog.d(TAG, "Dump Trace: add java proc " + javaPid);
                }
            }
        }
        if (scoutNativePids.size() > 0) {
            Iterator<Integer> it2 = scoutNativePids.iterator();
            while (it2.hasNext()) {
                int nativePid = it2.next().intValue();
                if (!nativePids.contains(Integer.valueOf(nativePid))) {
                    nativePids.add(Integer.valueOf(nativePid));
                    Slog.d(TAG, "Dump Trace: add java proc " + nativePid);
                }
            }
        }
    }

    public boolean scoutSystemMonitorEnable() {
        return ScoutHelper.ENABLED_SCOUT;
    }

    public void scoutSystemMonitorInit(Watchdog.Monitor monitor, Object mLock) {
        init(mLock);
        addScoutBinderMonitor(monitor);
    }

    public void scoutSystemMonitorWork(long timeout, int count, boolean waitedHalf, ScoutWatchdogInfo.ScoutId scoutId) {
        runSystemMonitor(timeout, count, waitedHalf, scoutId);
    }

    public void scoutSystemMonitorInitContext(Context context, ActivityManagerService activity) {
        this.mContext = context;
        this.mService = activity;
        registerScreenStateReceiver();
    }

    public void reportRebootNullEventtoMqs(String processName, int processId, String triggerWay, String intentName) {
        if (shouldRebootReasonCheckNull() && !SystemProperties.getBoolean("debug.record.rebootnull", false)) {
            String details = null;
            if (triggerWay.contains("intent")) {
                details = String.format("Shutdown intent checkpoint recorded intent=%s from package=%s", intentName, processName);
                SystemProperties.set("debug.record.rebootnull", "true");
            } else if (triggerWay.contains("binder")) {
                processName = ProcessUtils.getProcessNameByPid(processId);
                details = "Binder shutdown checkpoint recorded with package=" + processName;
            }
            String caller = processName + " by " + triggerWay;
            RebootNullEvent event = new RebootNullEvent();
            event.setProcessName(processName);
            event.setPackageName(processName);
            event.setTimeStamp(System.currentTimeMillis());
            event.setCaller(caller);
            event.setSummary("reboot or shutdown with null reason");
            event.setDetails(details);
            MQSEventManagerDelegate.getInstance().reportRebootNullEvent(event);
        }
    }

    public boolean shouldRebootReasonCheckNull() {
        return SystemProperties.getBoolean("persist.sys.stability.rebootreason_check", true);
    }

    public Handler getSystemWorkerHandler() {
        return this.mSysWorkerHandler;
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ScreenStateReceiver(), filter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ScreenStateReceiver extends BroadcastReceiver {
        ScreenStateReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            boolean screenOn = false;
            if (ScoutSystemMonitor.this.miuiFboService == null) {
                ScoutSystemMonitor.this.miuiFboService = (MiuiFboServiceInternal) LocalServices.getService(MiuiFboServiceInternal.class);
            }
            try {
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
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
                        screenOn = true;
                        if (ScoutSystemMonitor.this.miuiFboService.getGlobalSwitch() && ScoutSystemMonitor.this.miuiFboService.getNativeIsRunning()) {
                            ScoutSystemMonitor.this.miuiFboService.deliverMessage("stopDueToScreen", 5, 0L);
                            break;
                        }
                        break;
                    default:
                        screenOn = false;
                        break;
                }
                ScoutSystemMonitor.this.miuiFboService.setScreenStatus(screenOn);
            } catch (Exception e) {
                Slog.w(ScoutSystemMonitor.TAG, "brodacastReceiver exp ", e);
            }
            ScoutSystemMonitor.this.updateScreenState(screenOn);
        }
    }

    /* loaded from: classes.dex */
    public static class ScoutSystemInfo {
        private String mBinderTransInfo = "";
        private String mDescribeInfo;
        private String mDetails;
        private int mEvent;
        private ArrayList<ScoutHandlerChecker> mHandlerChecks;
        private boolean mIsHalf;
        private int mPreScoutLevel;
        private int mScoutLevel;
        private long mTimeStamp;
        private String mUuid;

        public ScoutSystemInfo(String mDescribeInfo, String mDetails, int mScoutLevel, int mPreScoutLevel, ArrayList<ScoutHandlerChecker> mHandlerChecks, boolean mIsHalf, String mUuid) {
            this.mDescribeInfo = mDescribeInfo;
            this.mDetails = mDetails;
            this.mScoutLevel = mScoutLevel;
            this.mPreScoutLevel = mPreScoutLevel;
            this.mIsHalf = mIsHalf;
            this.mHandlerChecks = mHandlerChecks;
            this.mUuid = mUuid;
        }

        public boolean getHalfState() {
            return this.mIsHalf;
        }

        public String getDescribeInfo() {
            return this.mDescribeInfo;
        }

        public int getScoutLevel() {
            return this.mScoutLevel;
        }

        public int getPreScoutLevel() {
            return this.mPreScoutLevel;
        }

        public ArrayList<ScoutHandlerChecker> getHandlerCheckers() {
            return this.mHandlerChecks;
        }

        public void setBinderTransInfo(String mBinderTransInfo) {
            this.mBinderTransInfo = mBinderTransInfo;
        }

        public String getBinderTransInfo() {
            return this.mBinderTransInfo;
        }

        public void setTimeStamp(long mTimeStamp) {
            this.mTimeStamp = mTimeStamp;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }

        public String getDetails() {
            return this.mDetails;
        }

        public void setEvent(int mEvent) {
            this.mEvent = mEvent;
        }

        public int getEvent() {
            return this.mEvent;
        }

        public String getUuid() {
            return this.mUuid;
        }

        public void setUuid(String mUuid) {
            this.mUuid = mUuid;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nException ： " + getEventString(this.mEvent) + "\nTimeStamp : " + getFormatDateTime(this.mTimeStamp) + "\nProcessName : system_server\nPid : " + Process.myPid() + "\nSummary : " + this.mDescribeInfo + "\n" + this.mBinderTransInfo + "\n");
            return sb.toString();
        }

        private String getFormatDateTime(long timeMillis) {
            if (timeMillis <= 0) {
                return "unknow time";
            }
            Date date = new Date(timeMillis);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            return dateFormat.format(date);
        }

        public String getEventString(int eventId) {
            switch (eventId) {
                case 2:
                    return "WATCHDOG";
                case 384:
                    return "HALF_WATCHDOG";
                case 385:
                    return "WATCHDOG_DUMP_ERROR";
                case 400:
                    return "FW_SCOUT_HANG";
                case 401:
                    return "FW_SCOUT_BINDER_FULL";
                case 402:
                    return "FW_SCOUT_NORMALLY";
                case 403:
                    return "FW_SCOUT_SLOW";
                default:
                    return "UNKNOW";
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SystemWorkerHandler extends Handler {
        static final int FW_SCOUT_BINDER_FULL = 1;
        static final int FW_SCOUT_HANG = 0;
        static final int FW_SCOUT_MEM_CHECK = 10;
        static final int FW_SCOUT_MEM_CRITICAL = 12;
        static final int FW_SCOUT_MEM_DUMP = 11;
        static final int FW_SCOUT_NORMALLY = 3;
        static final int FW_SCOUT_SLOW = 2;

        public SystemWorkerHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            TreeMap<Integer, Integer> inPidMap;
            ScoutSystemInfo mInfo;
            ArrayList<Integer> pids = new ArrayList<>(5);
            ArrayList<Integer> nativePids = new ArrayList<>(5);
            ScoutHelper.ScoutBinderInfo scoutBinderInfo = new ScoutHelper.ScoutBinderInfo(Process.myPid(), 0, "MIUIScout System");
            File mScoutStack = null;
            switch (msg.what) {
                case 0:
                    ScoutSystemInfo mInfo2 = (ScoutSystemInfo) msg.obj;
                    boolean mIsHalf = mInfo2.getHalfState();
                    pids.add(Integer.valueOf(Process.myPid()));
                    ScoutHelper.checkBinderCallPidList(Process.myPid(), scoutBinderInfo, pids, nativePids);
                    mInfo2.setBinderTransInfo(scoutBinderInfo.getBinderTransInfo() + "\n" + scoutBinderInfo.getProcInfo());
                    if (!mIsHalf || mInfo2.getScoutLevel() <= 2) {
                        mScoutStack = StackTracesDumpHelper.dumpStackTraces(pids, (ProcessCpuTracker) null, (SparseBooleanArray) null, CompletableFuture.completedFuture(nativePids), (StringWriter) null, mInfo2.toString(), (String) null, (Executor) null, (AnrLatencyTracker) null);
                    }
                    ScoutSystemMonitor.onFwScout(400, mScoutStack, mInfo2, "");
                    int monkeyPid = scoutBinderInfo.getMonkeyPid();
                    if (monkeyPid != 0) {
                        Process.killProcess(monkeyPid);
                        return;
                    }
                    return;
                case 1:
                    ScoutSystemInfo mInfo3 = (ScoutSystemInfo) msg.obj;
                    boolean mIsHalf2 = mInfo3.getHalfState();
                    pids.add(Integer.valueOf(Process.myPid()));
                    TreeMap<Integer, Integer> inPidMap2 = new TreeMap<>();
                    ScoutHelper.checkBinderCallPidList(Process.myPid(), scoutBinderInfo, pids, nativePids);
                    ScoutHelper.checkBinderThreadFull(Process.myPid(), scoutBinderInfo, inPidMap2, pids, nativePids);
                    mInfo3.setBinderTransInfo(scoutBinderInfo.getBinderTransInfo() + "\n" + scoutBinderInfo.getProcInfo());
                    if (!mIsHalf2 || mInfo3.getScoutLevel() <= 2) {
                        inPidMap = inPidMap2;
                        mInfo = mInfo3;
                        mScoutStack = StackTracesDumpHelper.dumpStackTraces(pids, (ProcessCpuTracker) null, (SparseBooleanArray) null, CompletableFuture.completedFuture(nativePids), (StringWriter) null, mInfo3.toString(), (String) null, (Executor) null, (AnrLatencyTracker) null);
                    } else {
                        inPidMap = inPidMap2;
                        mInfo = mInfo3;
                    }
                    String mOtherMsg = ScoutHelper.resumeBinderThreadFull(ScoutSystemMonitor.TAG, inPidMap);
                    ScoutSystemMonitor.onFwScout(401, mScoutStack, mInfo, mOtherMsg);
                    return;
                case 10:
                    ScoutDisplayMemoryManager.getInstance().checkScoutLowMemory();
                    return;
                case 11:
                    AppProfilerStub.getInstance().dumpProcsMemInfo();
                    return;
                case 12:
                    AppProfilerStub.getInstance().checkMemoryPsi(false);
                    return;
                default:
                    Slog.w(ScoutSystemMonitor.TAG, "    // wrong message received of WorkerHandler");
                    return;
            }
        }
    }

    public void setWorkMessage(int msgId) {
        Message msg;
        if (this.mSysWorkerHandler == null) {
            return;
        }
        switch (msgId) {
            case 0:
                msg = this.mSysWorkerHandler.obtainMessage(10);
                break;
            case 1:
                msg = this.mSysWorkerHandler.obtainMessage(11);
                break;
            case 2:
                msg = this.mSysWorkerHandler.obtainMessage(12);
                break;
            default:
                return;
        }
        this.mSysWorkerHandler.sendMessage(msg);
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:33:0x003a -> B:12:0x0055). Please report as a decompilation issue!!! */
    private String getScoutSystemDetailsAsync(final List<ScoutHandlerChecker> handlerCheckers) {
        String str = "getScoutSystemDetails shutdown exp ";
        if (ScoutUtils.isMtbfTest()) {
            return null;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() { // from class: com.android.server.ScoutSystemMonitor.1
            @Override // java.util.concurrent.Callable
            public String call() {
                StackTraceElement[] st;
                StringBuilder details = new StringBuilder();
                for (int i = 0; i < handlerCheckers.size(); i++) {
                    ScoutHandlerChecker mCheck = (ScoutHandlerChecker) handlerCheckers.get(i);
                    int tid = mCheck.getThreadTid();
                    if (!ScoutHelper.IS_INTERNATIONAL_BUILD && tid > 0) {
                        st = ScoutHelper.getMiuiStackTraceByTid(tid);
                    } else {
                        st = mCheck.getThread().getStackTrace();
                    }
                    if (st != null) {
                        for (StackTraceElement element : st) {
                            details.append("    at ").append(element).append("\n");
                        }
                    }
                    details.append("\n\n");
                }
                String stackTraces = details.toString();
                if (stackTraces == null) {
                    return null;
                }
                return new String(stackTraces);
            }
        });
        executor.execute(futureTask);
        String result = null;
        try {
            try {
                try {
                    result = futureTask.get(3000L, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Slog.w(TAG, "getScoutSystemDetails exp is ", e);
                    try {
                        futureTask.cancel(true);
                    } catch (Exception e1) {
                        Slog.w(TAG, "getScoutSystemDetails futureTask.cancel exp ", e1);
                        executor.shutdown();
                        str = "getScoutSystemDetails fail";
                        Slog.w(TAG, "getScoutSystemDetails fail");
                        return result;
                    }
                    executor.shutdown();
                }
            } catch (Exception e2) {
                Slog.w(TAG, str, e2);
            }
            if (result != null) {
                try {
                    executor.shutdown();
                } catch (Exception e3) {
                    Slog.w(TAG, "getScoutSystemDetails shutdown exp ", e3);
                }
                return result;
            }
            executor.shutdown();
            str = "getScoutSystemDetails fail";
            Slog.w(TAG, "getScoutSystemDetails fail");
            return result;
        } catch (Throwable th) {
            try {
                executor.shutdown();
            } catch (Exception e4) {
                Slog.w(TAG, "getScoutSystemDetails shutdown exp ", e4);
            }
            throw th;
        }
    }

    private String getScoutSystemDetails(List<ScoutHandlerChecker> handlerCheckers) {
        StackTraceElement[] st;
        if (ScoutUtils.isMtbfTest() || handlerCheckers == null) {
            return null;
        }
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < handlerCheckers.size(); i++) {
            ScoutHandlerChecker mCheck = handlerCheckers.get(i);
            int tid = mCheck.getThreadTid();
            if (!ScoutHelper.IS_INTERNATIONAL_BUILD && tid > 0) {
                st = ScoutHelper.getMiuiStackTraceByTid(tid);
            } else {
                st = mCheck.getThread().getStackTrace();
            }
            if (st != null) {
                for (StackTraceElement element : st) {
                    details.append("    at ").append(element).append("\n");
                }
            }
            details.append("\n\n");
        }
        return details.toString();
    }

    private void runSystemMonitor(long timeout, int count, boolean waitedHalf, ScoutWatchdogInfo.ScoutId scoutId) {
        long waitTime;
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        Message msg;
        if (timeout >= 10000) {
            waitTime = 10000;
        } else {
            waitTime = timeout;
        }
        int i = 0;
        while (true) {
            ArrayList<ScoutHandlerChecker> arrayList = mScoutHandlerCheckers;
            if (i >= arrayList.size()) {
                break;
            }
            ScoutHandlerChecker hc = arrayList.get(i);
            hc.scheduleCheckLocked();
            i++;
        }
        long scoutTimeout = waitTime;
        long scoutStart = SystemClock.uptimeMillis();
        for (long scoutTimeout2 = scoutTimeout; scoutTimeout2 > 0; scoutTimeout2 = waitTime - (SystemClock.uptimeMillis() - scoutStart)) {
            try {
                this.mScoutSysLock.wait(scoutTimeout2);
            } catch (InterruptedException ex) {
                Slog.wtf(TAG, ex);
            }
        }
        int waitState = evaluateCheckerScoutCompletionLocked();
        String uuid = scoutId.getUuid();
        int i2 = this.scoutLevel;
        this.preScoutLevel = i2;
        if (waitState == 3) {
            str = " to ";
            str2 = "; Scout Check count : ";
            str3 = "MIUIScout System";
            str4 = "ms";
            str5 = " waittime : ";
        } else {
            if (waitState != 2 || count != 2) {
                this.scoutLevel = 0;
                if (i2 > 1) {
                    ScoutSystemInfo info = new ScoutSystemInfo("", "", 0, i2, null, waitedHalf, uuid);
                    info.setTimeStamp(System.currentTimeMillis());
                    if (!waitedHalf) {
                        info.setEvent(403);
                        Slog.d("MIUIScout System", "Enter FW_SCOUT_SLOW from Level " + this.preScoutLevel + " to " + this.scoutLevel + "; Scout Check count : " + count + " waittime : " + waitTime + "ms");
                        onFwScout(403, null, info, "");
                    } else {
                        info.setEvent(402);
                        Slog.d("MIUIScout System", "Enter FW_SCOUT_NORMALLY from Level " + this.preScoutLevel + " to " + this.scoutLevel + "; Scout Check count : " + count + " waittime : " + waitTime + "ms");
                        onFwScout(402, null, info, "");
                    }
                } else if (i2 > 0) {
                    Slog.d("MIUIScout System", "FW Resume from Level " + this.preScoutLevel + " to " + this.scoutLevel + "; Scout Check count : " + count + " waittime : " + waitTime + "ms");
                }
                scoutId.setUuid(UUID.randomUUID().toString());
                return;
            }
            str = " to ";
            str2 = "; Scout Check count : ";
            str3 = "MIUIScout System";
            str4 = "ms";
            str5 = " waittime : ";
        }
        int i3 = i2 + 1;
        this.scoutLevel = i3;
        if ((count != 2 || waitedHalf || i3 <= 2) && (count != 2 || !waitedHalf || i3 <= 5)) {
            boolean isHalf = waitState == 2;
            ArrayList<ScoutHandlerChecker> handlerChecke = getScoutBlockedCheckersLocked(isHalf);
            String scoutDescribe = describeScoutCheckersLocked(handlerChecke);
            String scoutDetails = getScoutSystemDetailsAsync(handlerChecke);
            String str6 = str3;
            String str7 = str4;
            String str8 = str5;
            String str9 = str2;
            long waitTime2 = waitTime;
            String str10 = str;
            ScoutSystemInfo info2 = new ScoutSystemInfo(scoutDescribe, scoutDetails, this.scoutLevel, this.preScoutLevel, handlerChecke, waitedHalf, uuid);
            if (scoutDescribe.contains("BinderThreadMonitor")) {
                info2.setEvent(401);
                Slog.d(str6, "Enter FW_SCOUT_BINDER_FULL from Level " + this.preScoutLevel + str10 + this.scoutLevel + str9 + count + str8 + waitTime2 + str7);
                msg = this.mSysWorkerHandler.obtainMessage(1);
            } else {
                info2.setEvent(400);
                Slog.d(str6, "Enter FW_SCOUT_HANG from Level " + this.preScoutLevel + str10 + this.scoutLevel + str9 + count + str8 + waitTime2 + str7);
                msg = this.mSysWorkerHandler.obtainMessage(0);
            }
            info2.setTimeStamp(System.currentTimeMillis());
            msg.obj = info2;
            this.mSysWorkerHandler.sendMessage(msg);
        }
    }

    /* loaded from: classes.dex */
    public final class ScoutHandlerChecker implements Runnable {
        private Watchdog.Monitor mCurrentMonitor;
        private final Handler mHandler;
        private final String mName;
        private int mPauseCount;
        private long mStartTime;
        private final long mWaitMax;
        private final ArrayList<Watchdog.Monitor> mMonitors = new ArrayList<>();
        private final ArrayList<Watchdog.Monitor> mMonitorQueue = new ArrayList<>();
        private boolean mCompleted = true;

        ScoutHandlerChecker(Handler handler, String name, long waitMaxMillis) {
            this.mHandler = handler;
            this.mName = name;
            this.mWaitMax = waitMaxMillis;
        }

        void addMonitorLocked(Watchdog.Monitor monitor) {
            this.mMonitorQueue.add(monitor);
        }

        public void scheduleCheckLocked() {
            if (this.mCompleted) {
                this.mMonitors.addAll(this.mMonitorQueue);
                this.mMonitorQueue.clear();
            }
            if ((this.mMonitors.size() == 0 && this.mHandler.getLooper().getQueue().isPolling()) || this.mPauseCount > 0) {
                this.mCompleted = true;
            } else {
                if (!this.mCompleted) {
                    return;
                }
                this.mCompleted = false;
                this.mCurrentMonitor = null;
                this.mStartTime = SystemClock.uptimeMillis();
                this.mHandler.postAtFrontOfQueue(this);
            }
        }

        public boolean isOverdueLocked() {
            return !this.mCompleted && SystemClock.uptimeMillis() > this.mStartTime + this.mWaitMax;
        }

        public boolean isHalfLocked() {
            return !this.mCompleted && SystemClock.uptimeMillis() > this.mStartTime + (this.mWaitMax / 2);
        }

        public int getCompletionStateLocked() {
            if (this.mCompleted) {
                return 0;
            }
            long latency = SystemClock.uptimeMillis() - this.mStartTime;
            long j = this.mWaitMax;
            if (latency < j / 2) {
                return 1;
            }
            if (latency < j) {
                return 2;
            }
            return 3;
        }

        public Thread getThread() {
            return this.mHandler.getLooper().getThread();
        }

        public int getThreadTid() {
            MessageMonitor mMonitor = this.mHandler.getLooper().getMessageMonitor();
            if (mMonitor != null) {
                return mMonitor.getThreadTid();
            }
            return 0;
        }

        public String getName() {
            return this.mName;
        }

        public String describeBlockedStateLocked() {
            if (this.mCurrentMonitor == null) {
                return "Blocked in handler on " + this.mName + " (" + getThread().getName() + ")";
            }
            return "Blocked in monitor " + this.mCurrentMonitor.getClass().getName() + " on " + this.mName + " (" + getThread().getName() + ")";
        }

        @Override // java.lang.Runnable
        public void run() {
            Watchdog.Monitor monitor;
            int size = this.mMonitors.size();
            for (int i = 0; i < size; i++) {
                synchronized (ScoutSystemMonitor.this.mScoutSysLock) {
                    monitor = this.mMonitors.get(i);
                    this.mCurrentMonitor = monitor;
                }
                monitor.monitor();
            }
            synchronized (ScoutSystemMonitor.this.mScoutSysLock) {
                this.mCompleted = true;
                this.mCurrentMonitor = null;
            }
        }

        public void pauseLocked(String reason) {
            this.mPauseCount++;
            this.mCompleted = true;
            Slog.i(ScoutSystemMonitor.TAG, "Pausing HandlerChecker: " + this.mName + " for reason: " + reason + ". Pause count: " + this.mPauseCount);
        }

        public void resumeLocked(String reason) {
            int i = this.mPauseCount;
            if (i > 0) {
                this.mPauseCount = i - 1;
                Slog.i(ScoutSystemMonitor.TAG, "Resuming HandlerChecker: " + this.mName + " for reason: " + reason + ". Pause count: " + this.mPauseCount);
            } else {
                Slog.wtf(ScoutSystemMonitor.TAG, "Already resumed HandlerChecker: " + this.mName);
            }
        }
    }

    public void addScoutMonitor(Watchdog.Monitor monitor) {
        if (ScoutHelper.ENABLED_SCOUT) {
            synchronized (this) {
                this.mScoutMonitorChecker.addMonitorLocked(monitor);
            }
        }
    }

    private void addScoutBinderMonitor(Watchdog.Monitor monitor) {
        if (ScoutHelper.ENABLED_SCOUT) {
            synchronized (this) {
                this.mScoutBinderMonitorChecker.addMonitorLocked(monitor);
            }
        }
    }

    public void addScoutThread(Handler thread) {
        if (ScoutHelper.ENABLED_SCOUT) {
            synchronized (this) {
                String name = thread.getLooper().getThread().getName();
                mScoutHandlerCheckers.add(new ScoutHandlerChecker(thread, name, 10000L));
            }
        }
    }

    public static int evaluateCheckerScoutCompletionLocked() {
        int state = 0;
        int i = 0;
        while (true) {
            ArrayList<ScoutHandlerChecker> arrayList = mScoutHandlerCheckers;
            if (i < arrayList.size()) {
                ScoutHandlerChecker hc = arrayList.get(i);
                state = Math.max(state, hc.getCompletionStateLocked());
                i++;
            } else {
                return state;
            }
        }
    }

    public static ArrayList<ScoutHandlerChecker> getScoutBlockedCheckersLocked(boolean isHalf) {
        boolean debug = ScoutHelper.ENABLED_SCOUT_DEBUG;
        ArrayList<ScoutHandlerChecker> checkers = new ArrayList<>();
        int i = 0;
        while (true) {
            ArrayList<ScoutHandlerChecker> arrayList = mScoutHandlerCheckers;
            if (i < arrayList.size()) {
                ScoutHandlerChecker hc = arrayList.get(i);
                if (hc.isOverdueLocked() || (isHalf && hc.isHalfLocked())) {
                    if (debug) {
                        Slog.d(TAG, "Debug: getScoutCheckersLocked Block : " + hc.describeBlockedStateLocked());
                    }
                    checkers.add(hc);
                } else if (debug) {
                    Slog.d(TAG, "Debug: no Block getScoutCheckersLocked Block : " + hc.describeBlockedStateLocked());
                }
                i++;
            } else {
                return checkers;
            }
        }
    }

    public static String describeScoutCheckersLocked(List<ScoutHandlerChecker> checkers) {
        StringBuilder builder = new StringBuilder(128);
        for (int i = 0; i < checkers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            String info = checkers.get(i).describeBlockedStateLocked();
            builder.append(info);
        }
        return builder.toString();
    }

    public void pauseScoutWatchingCurrentThread(String reason) {
        if (ScoutHelper.ENABLED_SCOUT) {
            synchronized (this) {
                Iterator<ScoutHandlerChecker> it = mScoutHandlerCheckers.iterator();
                while (it.hasNext()) {
                    ScoutHandlerChecker hc = it.next();
                    if (Thread.currentThread().equals(hc.getThread())) {
                        hc.pauseLocked(reason);
                    }
                }
            }
        }
    }

    public void resumeScoutWatchingCurrentThread(String reason) {
        if (ScoutHelper.ENABLED_SCOUT) {
            synchronized (this) {
                Iterator<ScoutHandlerChecker> it = mScoutHandlerCheckers.iterator();
                while (it.hasNext()) {
                    ScoutHandlerChecker hc = it.next();
                    if (Thread.currentThread().equals(hc.getThread())) {
                        hc.resumeLocked(reason);
                    }
                }
            }
        }
    }

    public void updateScreenState(boolean screenOn) {
        AppScoutStateMachine appScoutStateMachine;
        if (!ScoutHelper.ENABLED_SCOUT || ScoutUtils.REBOOT_COREDUMP || ScoutUtils.MTBF_MIUI_TEST) {
            return;
        }
        if (screenOn && this.mUiScoutStateMachine == null) {
            this.mUiScoutStateMachine = AppScoutStateMachine.CreateAppScoutStateMachine(UiThread.get(), "UiThread", true);
        } else if (!screenOn && (appScoutStateMachine = this.mUiScoutStateMachine) != null) {
            appScoutStateMachine.quit();
            this.mUiScoutStateMachine = null;
        }
    }

    public AppScoutStateMachine getUiScoutStateMachine() {
        return this.mUiScoutStateMachine;
    }

    public boolean skipClipDataAppAnr(int pid, int uid) {
        String clipProcess = SystemProperties.get("persist.sys.debug.app.clipdata", "");
        if (TextUtils.isEmpty(clipProcess)) {
            return false;
        }
        String currentAnrApp = pid + d.h + uid;
        return currentAnrApp.equals(clipProcess);
    }

    public void resetClipProp(String packageName) {
        if (!TextUtils.isEmpty(packageName) && "com.milink.service".equals(packageName)) {
            SystemProperties.set("persist.sys.debug.app.clipdata", "");
        }
    }

    static void onFwScout(int type, File trace, ScoutSystemInfo info, String mOtherMsg) {
        if (Debug.isDebuggerConnected()) {
            return;
        }
        SysScoutEvent event = new SysScoutEvent();
        event.setType(type);
        event.setPid(Process.myPid());
        event.setProcessName("system_server");
        event.setPackageName("system_server");
        event.setTimeStamp(info.getTimeStamp());
        event.setSystem(true);
        event.setSummary(info.getDescribeInfo());
        if (info.getDetails() != null) {
            event.setDetails(info.getDetails());
        } else {
            event.setDetails(info.getDescribeInfo());
        }
        event.setOtherMsg(mOtherMsg);
        event.setScoutLevel(info.getScoutLevel());
        event.setPreScoutLevel(info.getPreScoutLevel());
        if (trace != null) {
            event.setLogName(trace.getAbsolutePath());
        }
        event.setBinderTransactionInfo(info.getBinderTransInfo());
        event.setUuid(info.getUuid());
        MQSEventManagerDelegate.getInstance().reportSysScoutEvent(event);
    }
}
