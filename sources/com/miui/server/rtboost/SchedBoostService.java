package com.miui.server.rtboost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MiuiProcess;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.perf.PerfMTKStub;
import android.perf.PerfStub;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.SystemPressureController;
import com.android.server.am.ThermalTempListener;
import com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda1;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.android.server.wm.RealTimeModeControllerStub;
import com.android.server.wm.SchedBoostGesturesEvent;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessListener;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import miui.util.FeatureParser;
import miui.util.ReflectionUtils;

/* loaded from: classes.dex */
public class SchedBoostService extends Binder implements SchedBoostManagerInternal {
    private static int[] ALL_CPU_CORES = null;
    private static final int BOOST_HOME_GPU_TIDS_MAX_RETRY = 3;
    private static final int[] CMDLINE_OUT;
    private static final int[] DEFAULT_QCOM_PERF_LIST;
    private static final int DEFAULT_UCLAMP_MAX = 1024;
    private static final int DEFAULT_UCLAMP_MIN = 0;
    private static final int DEFAULT_UCLAMP_UPPER = 614;
    private static final boolean ENABLE_RTMODE_UCLAMP;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    private static final boolean IS_MTK_DEVICE;
    public static final boolean IS_SCHED_HOME_GPU_THREADS_ENABLED;
    public static final boolean IS_SERVICE_ENABLED = true;
    public static final boolean IS_UCLAMP_ENABLED;
    public static final boolean IS_UIGROUP_ENABLED;
    private static final int[] MTK_GPU_AND_CPU_BOOST_LIST;
    private static final String PLATFORM_8650 = "pineapple";
    private static final int[] QCOM_GPU_AND_CPU_BOOST_LIST;
    private static final int[] QCOM_GPU_AND_CPU_HIGHER_FREQ;
    private static final long REPEAT_CALL_DURATION = 300;
    private static final String[] RT_THREAD_COMM_LIST;
    public static final String SERVICE_NAME = "SchedBoostService";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    public static final String TAG = "SchedBoost";
    public static final int THREAD_GROUP_UI = 10;
    private static final String THREAD_NAME = "SchedBoostServiceTh";
    private static final int THREAD_PRIORITY_HIGHEST = -20;
    private boolean isInited;
    private boolean isNormalPolicy;
    private Context mContext;
    private int[] mDefultPerfList;
    private int[] mGpuAndCpuPerfList;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private PerfMTKStub mMTKPerfStub;
    private PerfStub mQcomPerfStub;
    private SchedBoostGesturesEvent mSchedBoostGesturesEvent;
    private boolean mSimpleNotNeedSched;
    private WindowManagerService mWMS;
    public static final boolean DEBUG = RealTimeModeControllerImpl.DEBUG;
    private static final String PLATFORM_NAME = SystemProperties.get("ro.vendor.qti.soc_name");
    private int TASK_UCLAMP_MIN = SystemProperties.getInt("persist.sys.speedui_uclamp_min", DEFAULT_UCLAMP_UPPER);
    private int TASK_UCLAMP_MAX = SystemProperties.getInt("persist.sys.speedui_uclamp_max", 1024);
    private boolean mPendingHomeGpuTids = true;
    private int mBoostHomeGpuTidsTryCnt = 0;
    private int mHandle = 0;
    private long mLastPerfAcqTime = 0;
    private BoostingPackageMap mBoostingMap = new BoostingPackageMap();
    private final ArraySet<Integer> mAlwaysRtTids = new ArraySet<>();
    private final ArraySet<Integer> mRtTids = new ArraySet<>();
    private Method mMethodSetAffinity = null;
    private Method mMethodUclampTask = null;
    private String mPreBoostProcessName = null;
    private int mHomeMainThreadId = 0;
    private int mHomeRenderThreadId = 0;
    private int mSystemUIMainThreadId = 0;
    private int mSystemUIRenderThreadId = 0;
    private boolean mScreenOff = false;
    private final BroadcastReceiver mSysStatusReceiver = new BroadcastReceiver() { // from class: com.miui.server.rtboost.SchedBoostService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                SchedBoostService.this.mScreenOff = true;
                SchedBoostService.this.stopCurrentSchedBoost();
            } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                SchedBoostService.this.mScreenOff = false;
            }
        }
    };

    static {
        boolean z = FeatureParser.getBoolean("is_mediatek", false);
        IS_MTK_DEVICE = z;
        boolean z2 = SystemProperties.getBoolean("persist.sys.enable_rtmode_uclamp", false);
        ENABLE_RTMODE_UCLAMP = z2;
        IS_UCLAMP_ENABLED = z && z2;
        IS_UIGROUP_ENABLED = SystemProperties.getBoolean("persist.sys.enable_setuicgroup", false);
        IS_SCHED_HOME_GPU_THREADS_ENABLED = SystemProperties.getBoolean("persist.sys.enable_sched_gpu_threads", false);
        CMDLINE_OUT = new int[]{4096};
        ALL_CPU_CORES = null;
        RT_THREAD_COMM_LIST = new String[]{"mali-event-hand", "mali-cpu-comman", "ged-swd"};
        DEFAULT_QCOM_PERF_LIST = new int[]{1082130432, 1500, 1115701248, 0, 1120010240, 1};
        QCOM_GPU_AND_CPU_BOOST_LIST = new int[]{1077936129, 1, 1086324736, 1, 1082146816, 4095, 1082147072, 4095, 1082130432, 1700, 1082130688, 1700, 1115701248, 0, 1120010240, 1};
        QCOM_GPU_AND_CPU_HIGHER_FREQ = new int[]{1077936129, 1, 1086324736, 1, 1082146816, 4095, 1082147072, 4095, 1082130432, 2300, 1082130688, 4095, 1115701248, 0, 1120010240, 1};
        MTK_GPU_AND_CPU_BOOST_LIST = new int[]{4194304, 1800000, 4194560, 2000000, 4194816, 2000000, 21201920, 100, 21202176, 100, 12582912, 35, 12632576, 18743356, 12632832, 771, 20988160, 1, 20988672, 0, 20988928, 1};
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final SchedBoostService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new SchedBoostService(context);
        }

        public void onStart() {
            publishBinderService(SchedBoostService.SERVICE_NAME, this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 1000) {
                RealTimeModeControllerStub.get().onBootPhase();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public SchedBoostService(Context context) {
        Object[] objArr = 0;
        this.mContext = context;
        if (MiuiProcess.PROPERTY_CPU_CORE_COUNT <= 0) {
            return;
        }
        ALL_CPU_CORES = new int[MiuiProcess.PROPERTY_CPU_CORE_COUNT];
        for (int i = 0; i < MiuiProcess.PROPERTY_CPU_CORE_COUNT; i++) {
            ALL_CPU_CORES[i] = i;
        }
        this.mWMS = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        if (RealTimeModeControllerImpl.ENABLE_RT_MODE && init()) {
            HandlerThread handlerThread = new HandlerThread(THREAD_NAME) { // from class: com.miui.server.rtboost.SchedBoostService.2
                @Override // android.os.HandlerThread
                protected void onLooperPrepared() {
                    super.onLooperPrepared();
                    int tid = getThreadId();
                    ActivityManagerService.scheduleAsFifoPriority(tid, true);
                }
            };
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
            LocalServices.addService(SchedBoostManagerInternal.class, new LocalService());
            RealTimeModeControllerStub.get().init(this.mContext);
            RealTimeModeControllerStub.get().setWindowManager(this.mWMS);
            SchedBoostGesturesEvent schedBoostGesturesEvent = new SchedBoostGesturesEvent(this.mHandlerThread.getLooper());
            this.mSchedBoostGesturesEvent = schedBoostGesturesEvent;
            schedBoostGesturesEvent.init(context);
            this.mSchedBoostGesturesEvent.setGesturesEventListener(new SchedBoostGesturesEvent.GesturesEventListener() { // from class: com.miui.server.rtboost.SchedBoostService.3
                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onFling(float velocityX, float velocityY, int durationMs) {
                    RealTimeModeControllerImpl.get().onFling(durationMs);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onScroll(boolean started) {
                    Trace.traceBegin(4L, "onScroll");
                    RealTimeModeControllerImpl.get().onScroll(started);
                    Trace.traceEnd(4L);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onDown() {
                    Trace.traceBegin(4L, "onDown");
                    RealTimeModeControllerImpl.get().onDown();
                    Trace.traceEnd(4L);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onMove() {
                    Trace.traceBegin(4L, "onMove");
                    if (SchedBoostService.this.mBoostingMap.isEmpty()) {
                        RealTimeModeControllerImpl.get().onMove();
                    }
                    Trace.traceEnd(4L);
                }
            });
            SystemPressureController.getInstance().registerThermalTempListener(new ThermalTempListener() { // from class: com.miui.server.rtboost.SchedBoostService$$ExternalSyntheticLambda0
                public final void onThermalTempChange(int i2) {
                    SchedBoostService.this.lambda$new$0(i2);
                }
            });
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            context.registerReceiver(this.mSysStatusReceiver, intentFilter);
        }
    }

    private boolean init() {
        if (IS_MTK_DEVICE) {
            this.mMTKPerfStub = PerfMTKStub.newInstance();
            this.mDefultPerfList = PerfMTKStub.newInstance().getBoostParamByDevice();
            this.mGpuAndCpuPerfList = MTK_GPU_AND_CPU_BOOST_LIST;
        } else {
            this.mQcomPerfStub = PerfStub.newInstance();
            this.mDefultPerfList = DEFAULT_QCOM_PERF_LIST;
            this.mGpuAndCpuPerfList = QCOM_GPU_AND_CPU_BOOST_LIST;
        }
        Method tryFindMethodBestMatch = ReflectionUtils.tryFindMethodBestMatch(Process.class, "setSchedAffinity", new Class[]{Integer.TYPE, int[].class});
        this.mMethodSetAffinity = tryFindMethodBestMatch;
        if (tryFindMethodBestMatch == null) {
            Slog.e(TAG, "failed to find setSchedAffinity method");
            return false;
        }
        this.mMethodUclampTask = ReflectionUtils.tryFindMethodBestMatch(Process.class, "setTaskUclamp", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE});
        synchronized (SchedBoostService.class) {
            if (!this.isInited) {
                this.isInited = true;
            }
        }
        return true;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void schedProcessBoost(WindowProcessListener proc, String procName, int pid, int rtid, int schedMode, long timeout) {
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void setRenderThreadTid(WindowProcessController wpc) {
        if (wpc == null) {
            return;
        }
        if (RealTimeModeControllerImpl.isHomeProcess(wpc)) {
            this.mHomeMainThreadId = wpc.getPid();
            this.mHomeRenderThreadId = wpc.mRenderThreadTid;
            this.mPendingHomeGpuTids = true;
            this.mBoostHomeGpuTidsTryCnt = 0;
            return;
        }
        if (RealTimeModeControllerImpl.isSystemUIProcess(wpc)) {
            this.mSystemUIMainThreadId = wpc.getPid();
            this.mSystemUIRenderThreadId = wpc.mRenderThreadTid;
        }
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void enableSchedBoost(boolean enable) {
        Slog.w(TAG, Binder.getCallingPid() + " set sched boost enable:" + enable);
        this.mSimpleNotNeedSched = !enable;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void beginSchedThreads(int[] tids, long duration, String procName, int mode) {
        if (DEBUG) {
            Slog.d(TAG, "beginSchedThreads called, mode: " + mode + ", procName :" + procName);
        }
        Handler handler = this.mHandler;
        if (handler == null || tids == null || tids.length == 0) {
            return;
        }
        switch (mode) {
            case 3:
                handler.obtainSetThreadsAlwaysFIFOMsg(tids).sendToTarget();
                return;
            case 7:
                handler.obtainThreadBindBigCoreMsg(tids, duration).sendToTarget();
                return;
            case 9:
                procName = "com.miui.home";
                break;
            case 12:
                handler.obtainSetThreadsFIFOMsg(tids, duration, mode).sendToTarget();
                return;
            case 13:
                for (int tid : tids) {
                    if (tid > 0) {
                        this.mHandler.obtainReSetThreadsFIFOMsg(tid, duration, mode).sendToTarget();
                    }
                }
                return;
            case 101:
            case 102:
                handler.obtainBoostFreqMsg(tids, duration, procName, mode).sendToTarget();
                return;
        }
        if (duration >= 0 && duration <= 6000) {
            handler.obtainBeginSchedThreadsMsg(tids, duration, procName, mode).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBeginSchedThread(int[] tids, long duration, String procName, int mode) {
        String str = this.mPreBoostProcessName;
        if (str != null && !TextUtils.equals(str, procName)) {
            synchronized (this.mBoostingMap.mMapLock) {
                ArrayList<BoostThreadInfo> list = this.mBoostingMap.getList();
                if (list != null && list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        BoostThreadInfo threadInfo = list.get(i);
                        if ((threadInfo.mode == 4 || threadInfo.mode == 5) && mode < threadInfo.mode) {
                            return;
                        }
                        this.mHandler.removeResetThreadsMsg(threadInfo);
                        this.mHandler.obtainResetSchedThreadsMsg(threadInfo).sendToTarget();
                    }
                }
                this.mBoostingMap.clear();
            }
        }
        this.mPreBoostProcessName = procName;
        ArrayList<BoostThreadInfo> curList = getCurrentBoostThreadsList(tids, duration, procName, mode);
        if (curList != null) {
            int curListLength = curList.size();
            for (int i2 = 0; i2 < curListLength; i2++) {
                this.mHandler.obtainSchedThreadsMsg(curList.get(i2)).sendToTarget();
            }
        }
        if (IS_MTK_DEVICE && IS_SCHED_HOME_GPU_THREADS_ENABLED && this.mPendingHomeGpuTids && mode == 0 && !procName.isEmpty() && tids[0] > 0 && this.mBoostHomeGpuTidsTryCnt < 3) {
            if ("com.miui.home".equals(procName) || "com.mi.android.globallauncher".equals(procName)) {
                schedGpuThreads(tids[0], procName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSchedThread(BoostThreadInfo threadInfo) {
        if (threadInfo == null) {
            Slog.e(TAG, String.format("handleSchedThreads err: threadInfo null object", new Object[0]));
            return;
        }
        this.mHandler.removeResetThreadsMsg(threadInfo);
        Message resetMsg = this.mHandler.obtainResetSchedThreadsMsg(threadInfo);
        this.mHandler.sendMessageDelayed(resetMsg, threadInfo.duration);
        if (threadInfo.duration == 0) {
            return;
        }
        if (TextUtils.equals(PLATFORM_NAME, PLATFORM_8650)) {
            startBoostInternalWithOpcode(threadInfo);
        } else {
            startBoostInternal(threadInfo);
        }
        int tid = threadInfo.tid;
        long boostStartTime = threadInfo.boostStartTime;
        if (boostStartTime != 0) {
            if (DEBUG) {
                Slog.d(TAG, String.format("handleSchedThreads continue: threads: %s", Integer.valueOf(tid)));
                return;
            }
            return;
        }
        threadInfo.boostStartTime = SystemClock.uptimeMillis();
        threadInfo.savedPriority = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
        boolean forkPriority = false;
        int[] coresIndex = MiuiProcess.BIG_CORES_INDEX;
        if (threadInfo.tid == this.mHomeRenderThreadId || threadInfo.tid == this.mSystemUIRenderThreadId) {
            forkPriority = true;
        }
        if (MiuiProcess.BIG_PRIME_CORES_INDEX != null && threadInfo.mode == 0) {
            coresIndex = MiuiProcess.BIG_PRIME_CORES_INDEX;
        }
        setAffinityAndPriority(threadInfo, true, forkPriority, coresIndex);
        setTaskUclamp(tid, this.TASK_UCLAMP_MIN, this.TASK_UCLAMP_MAX);
        setTaskGroup(tid, 10);
        threadInfo.boostPriority = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
        if (DEBUG) {
            Slog.d(TAG, String.format("handleSchedThreads begin: threads: %s, procName: %s, mode: %s, aff:%s", Integer.valueOf(tid), threadInfo.procName, Integer.valueOf(threadInfo.mode), Arrays.toString(coresIndex)));
        }
        Trace.asyncTraceBegin(64L, "SchedBoost: " + threadInfo.procName, threadInfo.tid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00b2, code lost:
    
        android.util.Slog.d(com.miui.server.rtboost.SchedBoostService.TAG, "reset tid " + r6 + " group to foreground");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void handleResetSchedThread(com.miui.server.rtboost.SchedBoostService.BoostThreadInfo r12) {
        /*
            Method dump skipped, instructions count: 321
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.rtboost.SchedBoostService.handleResetSchedThread(com.miui.server.rtboost.SchedBoostService$BoostThreadInfo):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSetThreadsAlwaysFIFO(int[] tids) {
        for (int tid : tids) {
            if (tid > 0) {
                if (DEBUG) {
                    Slog.d(TAG, String.format("handleSetThreadsAlwaysFIFO, threads: %s", Integer.valueOf(tid)));
                }
                ActivityManagerService.scheduleAsFifoPriority(tid, true);
                this.mAlwaysRtTids.add(Integer.valueOf(tid));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSetThreadsFIFO(int tid, long duration, int mode) {
        this.mHandler.removeReSetThreadsFIFOMsg(Integer.valueOf(tid));
        Message resetMsg = this.mHandler.obtainReSetThreadsFIFOMsg(tid, duration, mode);
        this.mHandler.sendMessageDelayed(resetMsg, duration);
        if (DEBUG) {
            Slog.d(TAG, String.format("handleSetThreadsFIFO, threads: %s", Integer.valueOf(tid)));
        }
        MiuiProcess.scheduleAsFifoPriority(tid, 3, true);
        this.mRtTids.add(Integer.valueOf(tid));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReSetThreadsFIFO(int tid, long duration, int mode) {
        this.mHandler.removeReSetThreadsFIFOMsg(Integer.valueOf(tid));
        if (DEBUG) {
            Slog.d(TAG, String.format("handleReSetThreadsFIFO, threads: %s", Integer.valueOf(tid)));
        }
        if (this.mRtTids.contains(Integer.valueOf(tid))) {
            ActivityManagerService.scheduleAsRegularPriority(tid, true);
            this.mRtTids.remove(Integer.valueOf(tid));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleThreadsBindBigCoreMsg(Integer tid, long duration) {
        this.mHandler.removeUnBindBigCoreMsg(tid);
        Message resetMsg = this.mHandler.obtainThreadUnBindBigCoreMsg(tid);
        this.mHandler.sendMessageDelayed(resetMsg, duration);
        setSchedAffinity(tid.intValue(), MiuiProcess.BIG_CORES_INDEX);
        if (DEBUG) {
            Slog.d(TAG, String.format("BindBigCore, threads: %s, aff:%s", Integer.valueOf(tid.intValue()), Arrays.toString(MiuiProcess.BIG_CORES_INDEX)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleThreadsUnBindBigCoreMsg(Integer tid) {
        if (tid == null) {
            return;
        }
        this.mHandler.removeUnBindBigCoreMsg(tid);
        setSchedAffinity(tid.intValue(), ALL_CPU_CORES);
        if (DEBUG) {
            Slog.d(TAG, String.format("unBindBigCore, threads: %s, aff:%s", Integer.valueOf(tid.intValue()), Arrays.toString(ALL_CPU_CORES)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBoostFreq(int[] tids, long duration, String procName, int mode) {
        if (IS_MTK_DEVICE) {
            this.mMTKPerfStub.perfLockAcquire((int) duration, this.mDefultPerfList);
            return;
        }
        switch (mode) {
            case 101:
                this.mQcomPerfStub.perfWarmLaunchBoost(procName, (int) duration, 1);
                return;
            case 102:
                this.mQcomPerfStub.perfColdLaunchBoost(procName, (int) duration, 1);
                return;
            default:
                Slog.d(TAG, "BoostFreq mode can not match with COLD_LAUNCH or WARM_LAUNCH");
                return;
        }
    }

    private void schedGpuThreads(int pid, String procName) {
        ArrayList<Integer> targetTids = new ArrayList<>();
        String filePath = "/proc/" + pid + "/task";
        int[] tids = Process.getPids(filePath, new int[1024]);
        if (tids == null || tids.length == 0) {
            return;
        }
        for (int tmpTid : tids) {
            if (tmpTid >= 0) {
                String filePath2 = "/proc/" + pid + "/task/" + tmpTid + "/comm";
                String taskName = readCmdlineFromProcfs(filePath2);
                if (taskName != null && !taskName.equals("")) {
                    for (String comm : RT_THREAD_COMM_LIST) {
                        if (taskName.trim().equals(comm)) {
                            targetTids.add(Integer.valueOf(tmpTid));
                            if (DEBUG) {
                                Slog.d(TAG, "Sched GPU tid: " + tmpTid + " " + comm.trim());
                            }
                        }
                    }
                }
            }
        }
        beginSchedThreads(targetTids.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray(), 0L, procName, 3);
        if (targetTids.size() == RT_THREAD_COMM_LIST.length) {
            this.mPendingHomeGpuTids = false;
        } else {
            this.mBoostHomeGpuTidsTryCnt++;
        }
    }

    private String readCmdlineFromProcfs(String filePath) {
        String[] cmdline = new String[1];
        if (!Process.readProcFile(filePath, CMDLINE_OUT, cmdline, null, null)) {
            return "";
        }
        return cmdline[0];
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Handler extends android.os.Handler {
        private final int MSG_BEGIN_SCHED_THREADS;
        private final int MSG_BOOST_FREQ;
        private final int MSG_RESET_SCHED_THREADS;
        private final int MSG_RESET_THREAD_FIFO;
        private final int MSG_SCHED_THREADS;
        private final int MSG_SET_THREAD_ALWAYS_FIFO;
        private final int MSG_SET_THREAD_BIND_BIG_CORE;
        private final int MSG_SET_THREAD_FIFO;
        private final int MSG_SET_THREAD_UN_BIND_BIG_CORE;

        public Handler(Looper looper) {
            super(looper);
            this.MSG_SCHED_THREADS = 1;
            this.MSG_RESET_SCHED_THREADS = 2;
            this.MSG_BEGIN_SCHED_THREADS = 3;
            this.MSG_SET_THREAD_ALWAYS_FIFO = 4;
            this.MSG_SET_THREAD_BIND_BIG_CORE = 5;
            this.MSG_SET_THREAD_UN_BIND_BIG_CORE = 6;
            this.MSG_SET_THREAD_FIFO = 7;
            this.MSG_RESET_THREAD_FIFO = 8;
            this.MSG_BOOST_FREQ = 9;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = 0;
            switch (msg.what) {
                case 1:
                    BoostThreadInfo threadInfo = (BoostThreadInfo) msg.obj;
                    SchedBoostService.this.handleSchedThread(threadInfo);
                    return;
                case 2:
                    BoostThreadInfo threadInfo2 = (BoostThreadInfo) msg.obj;
                    SchedBoostService.this.handleResetSchedThread(threadInfo2);
                    return;
                case 3:
                    Bundle bundle = msg.getData();
                    String procName = bundle.getString("name");
                    int[] tids = bundle.getIntArray("tids");
                    int mode = bundle.getInt("mode");
                    long duration = bundle.getLong("timeout");
                    Trace.traceBegin(64L, "beginSchedThread, mode: " + mode);
                    SchedBoostService.this.handleBeginSchedThread(tids, duration, procName, mode);
                    Trace.traceEnd(64L);
                    return;
                case 4:
                    int[] tids2 = msg.getData().getIntArray("tids");
                    if (tids2 != null && tids2.length > 0) {
                        SchedBoostService.this.handleSetThreadsAlwaysFIFO(tids2);
                        return;
                    }
                    return;
                case 5:
                    Bundle bundle2 = msg.getData();
                    int[] tids3 = bundle2.getIntArray("tids");
                    long duration2 = bundle2.getLong("duration");
                    if (tids3 != null && tids3.length > 0) {
                        int length = tids3.length;
                        while (i < length) {
                            int tid = tids3[i];
                            SchedBoostService.this.handleThreadsBindBigCoreMsg(Integer.valueOf(tid), duration2);
                            i++;
                        }
                        return;
                    }
                    return;
                case 6:
                    Integer tid2 = (Integer) msg.obj;
                    SchedBoostService.this.handleThreadsUnBindBigCoreMsg(tid2);
                    return;
                case 7:
                    Bundle bundle3 = msg.getData();
                    int[] tids4 = bundle3.getIntArray("tids");
                    long duration3 = bundle3.getLong("timeout");
                    int mode2 = bundle3.getInt("mode");
                    if (tids4 != null && tids4.length > 0) {
                        int length2 = tids4.length;
                        while (i < length2) {
                            int tid3 = tids4[i];
                            SchedBoostService.this.handleSetThreadsFIFO(tid3, duration3, mode2);
                            i++;
                        }
                        return;
                    }
                    return;
                case 8:
                    Bundle bundle4 = msg.getData();
                    int tid4 = bundle4.getInt("tid");
                    long duration4 = bundle4.getLong("timeout");
                    SchedBoostService.this.handleReSetThreadsFIFO(tid4, duration4, bundle4.getInt("mode"));
                    return;
                case 9:
                    Bundle bundle5 = msg.getData();
                    String procName2 = bundle5.getString("name");
                    int[] tids5 = bundle5.getIntArray("tids");
                    bundle5.getLong("timeout");
                    int mode3 = bundle5.getInt("mode");
                    Trace.traceBegin(64L, "beginSchedThread, mode: " + mode3);
                    SchedBoostService.this.handleBoostFreq(tids5, -1L, procName2, mode3);
                    Trace.traceEnd(64L);
                    return;
                default:
                    return;
            }
        }

        public Message obtainSchedThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(1, threadInfo);
            msg.obj = threadInfo;
            return msg;
        }

        public Message obtainResetSchedThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(2, threadInfo);
            msg.obj = threadInfo;
            return msg;
        }

        public Message obtainBeginSchedThreadsMsg(int[] tids, long duration, String procName, int mode) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(3);
            Bundle data = new Bundle();
            data.putString("name", procName);
            data.putIntArray("tids", tids);
            data.putInt("mode", mode);
            data.putLong("timeout", duration);
            msg.setData(data);
            return msg;
        }

        public Message obtainSetThreadsAlwaysFIFOMsg(int[] tids) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(4);
            Bundle data = new Bundle();
            data.putIntArray("tids", tids);
            msg.setData(data);
            return msg;
        }

        public Message obtainReSetThreadsFIFOMsg(int tid, long duration, int mode) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(8);
            Bundle data = new Bundle();
            data.putInt("tid", tid);
            data.putLong("timeout", duration);
            data.putInt("mode", mode);
            msg.setData(data);
            return msg;
        }

        public Message obtainSetThreadsFIFOMsg(int[] tids, long duration, int mode) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(7);
            Bundle data = new Bundle();
            data.putIntArray("tids", tids);
            data.putLong("timeout", duration);
            data.putInt("mode", mode);
            msg.setData(data);
            return msg;
        }

        public Message obtainThreadBindBigCoreMsg(int[] tids, long duration) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(5);
            Bundle data = new Bundle();
            data.putIntArray("tids", tids);
            data.putLong("duration", duration);
            msg.setData(data);
            return msg;
        }

        public Message obtainThreadUnBindBigCoreMsg(Integer tid) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(6);
            msg.obj = tid;
            return msg;
        }

        public void removeResetThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            removeEqualMessages(2, threadInfo);
        }

        public void removeUnBindBigCoreMsg(Integer tid) {
            Objects.requireNonNull(this);
            removeEqualMessages(6, tid);
        }

        public void removeReSetThreadsFIFOMsg(Integer tid) {
            Objects.requireNonNull(this);
            removeEqualMessages(8, tid);
        }

        public Message obtainBoostFreqMsg(int[] tids, long duration, String procName, int mode) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(9);
            Bundle data = new Bundle();
            data.putString("name", procName);
            data.putIntArray("tids", tids);
            data.putLong("timeout", duration);
            data.putInt("mode", mode);
            msg.setData(data);
            return msg;
        }
    }

    private ArrayList<BoostThreadInfo> getCurrentBoostThreadsList(int[] tids, long duration, String procName, int mode) {
        ArrayList<BoostThreadInfo> curList = new ArrayList<>();
        for (int tid : tids) {
            if (tid > 0) {
                if (this.mAlwaysRtTids.contains(Integer.valueOf(tid))) {
                    if (DEBUG) {
                        Slog.d(TAG, "already rttids:" + this.mAlwaysRtTids.toString() + ",tid :" + tid);
                    }
                } else {
                    BoostThreadInfo threadInfo = new BoostThreadInfo(tid, duration, procName, mode);
                    this.mBoostingMap.put(threadInfo);
                    curList.add(threadInfo);
                }
            }
        }
        return curList;
    }

    /* loaded from: classes.dex */
    private final class LocalService implements SchedBoostManagerInternal {
        private LocalService() {
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void schedProcessBoost(WindowProcessListener proc, String procName, int pid, int rtid, int schedMode, long timeout) {
            SchedBoostService.this.schedProcessBoost(proc, procName, pid, rtid, schedMode, timeout);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void enableSchedBoost(boolean enable) {
            SchedBoostService.this.enableSchedBoost(enable);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void beginSchedThreads(int[] tids, long duration, String procName, int mode) {
            SchedBoostService.this.beginSchedThreads(tids, duration, procName, mode);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void setRenderThreadTid(WindowProcessController wpc) {
            SchedBoostService.this.setRenderThreadTid(wpc);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public boolean checkThreadBoost(int tid) {
            return SchedBoostService.this.checkThreadBoost(tid);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void setThreadSavedPriority(int[] tid, int prio) {
            SchedBoostService.this.setThreadSavedPriority(tid, prio);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void stopCurrentSchedBoost() {
            SchedBoostService.this.stopCurrentSchedBoost();
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void boostHomeAnim(long duration, int mode) {
            SchedBoostService.this.boostHomeAnim(duration, mode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BoostThreadInfo {
        private static final int DEFAULT_SAVED_PRIO = 0;
        private static final int DEFAULT_START_TIME = 0;
        int boostPriority;
        long boostStartTime;
        long duration;
        int mode;
        String procName;
        int savedPriority;
        int tid;

        public BoostThreadInfo(int tid, long duration, String procName, int mode) {
            this.tid = tid;
            this.duration = duration;
            this.procName = procName;
            this.mode = mode;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoostThreadInfo)) {
                return false;
            }
            BoostThreadInfo threadInfo = (BoostThreadInfo) obj;
            return threadInfo.tid == this.tid;
        }

        public void dump(PrintWriter pw, String[] args) {
            pw.println("#" + this.procName + " tid:" + this.tid + " duration: " + this.duration + " boostStartTime: " + this.boostStartTime + " mode: " + this.mode + " savedPriority: " + this.savedPriority + " boostPriority: " + this.boostPriority);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BoostingPackageMap {
        private final ArrayList<BoostThreadInfo> infoList;
        private final HashMap<Integer, BoostThreadInfo> mMap;
        private final Object mMapLock;

        private BoostingPackageMap() {
            this.mMap = new HashMap<>();
            this.infoList = new ArrayList<>();
            this.mMapLock = new Object();
        }

        public ArrayList<BoostThreadInfo> getList() {
            return this.infoList;
        }

        public void put(BoostThreadInfo threadInfo) {
            synchronized (this.mMapLock) {
                if (this.infoList.contains(threadInfo)) {
                    BoostThreadInfo oldThreadInfo = this.mMap.get(Integer.valueOf(threadInfo.tid));
                    threadInfo.boostStartTime = oldThreadInfo.boostStartTime;
                    threadInfo.savedPriority = oldThreadInfo.savedPriority;
                    threadInfo.boostPriority = oldThreadInfo.boostPriority;
                    removeByTid(oldThreadInfo.tid);
                }
                this.infoList.add(threadInfo);
                this.mMap.put(Integer.valueOf(threadInfo.tid), threadInfo);
            }
        }

        public void removeByTid(int tid) {
            synchronized (this.mMapLock) {
                BoostThreadInfo threadInfo = this.mMap.remove(Integer.valueOf(tid));
                if (threadInfo != null) {
                    this.infoList.remove(threadInfo);
                }
            }
        }

        public void clear() {
            synchronized (this.mMapLock) {
                this.mMap.clear();
                this.infoList.clear();
            }
        }

        public BoostThreadInfo getByTid(int tid) {
            BoostThreadInfo boostThreadInfo;
            synchronized (this.mMapLock) {
                boostThreadInfo = this.mMap.get(Integer.valueOf(tid));
            }
            return boostThreadInfo;
        }

        public boolean isEmpty() {
            boolean z;
            synchronized (this.mMapLock) {
                z = this.infoList.size() == 0 && this.mMap.isEmpty();
            }
            return z;
        }
    }

    private void setAffinityAndPriority(BoostThreadInfo threadInfo, boolean isFifo, boolean forkPriority, int[] coresIndex) {
        int tid = threadInfo.tid;
        boolean setFifo = false;
        if (isFifo) {
            if (this.isNormalPolicy) {
                setFifo = MiuiProcess.setThreadPriority(tid, THREAD_PRIORITY_HIGHEST, TAG);
            } else if (forkPriority) {
                setFifo = MiuiProcess.scheduleAsFifoAndForkPriority(tid, true);
            } else {
                setFifo = ActivityManagerService.scheduleAsFifoPriority(tid, true);
            }
        } else {
            int curPrio = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
            if (!this.isNormalPolicy || curPrio == threadInfo.boostPriority) {
                setFifo = ActivityManagerService.scheduleAsRegularPriority(tid, true);
                MiuiProcess.setThreadPriority(tid, threadInfo.savedPriority, TAG);
            }
        }
        if (setFifo) {
            setSchedAffinity(tid, coresIndex);
        }
        if (DEBUG) {
            Slog.d(TAG, String.format("setAffinityAndPriority, tid: %s, isFifo: %s, forkPriority: %s, coresIndex: %s", Integer.valueOf(tid), Boolean.valueOf(isFifo), Boolean.valueOf(forkPriority), Arrays.toString(coresIndex)));
        }
    }

    private void setSchedAffinity(int pid, int[] cores) {
        Method method = this.mMethodSetAffinity;
        if (method != null) {
            try {
                method.invoke(null, Integer.valueOf(pid), cores);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setTaskUclamp(int tid, int perfIdx, int maxIdx) {
        Method method;
        if (IS_UCLAMP_ENABLED && (method = this.mMethodUclampTask) != null) {
            try {
                method.invoke(null, Integer.valueOf(tid), Integer.valueOf(perfIdx), Integer.valueOf(maxIdx));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setTaskGroup(int tid, int group) {
        if (IS_UIGROUP_ENABLED) {
            Slog.i(TAG, "setTaskGroup: " + tid + ", " + group);
            try {
                Process.setThreadGroupAndCpuset(tid, group);
                Slog.i(TAG, String.format("setTaskGroup: Check cpuset of tid: %s,group= %s", Integer.valueOf(tid), Integer.valueOf(Process.getCpusetThreadGroup(tid))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0036 A[Catch: all -> 0x00a2, Exception -> 0x00a4, TryCatch #1 {Exception -> 0x00a4, blocks: (B:4:0x0002, B:6:0x0007, B:8:0x000c, B:11:0x0012, B:13:0x0017, B:14:0x0032, B:16:0x0036, B:18:0x0043, B:20:0x0047, B:24:0x0061, B:26:0x0068, B:27:0x007f, B:30:0x0074, B:31:0x001a, B:33:0x001e, B:36:0x0027, B:38:0x002d, B:39:0x0030), top: B:3:0x0002, outer: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0068 A[Catch: all -> 0x00a2, Exception -> 0x00a4, TryCatch #1 {Exception -> 0x00a4, blocks: (B:4:0x0002, B:6:0x0007, B:8:0x000c, B:11:0x0012, B:13:0x0017, B:14:0x0032, B:16:0x0036, B:18:0x0043, B:20:0x0047, B:24:0x0061, B:26:0x0068, B:27:0x007f, B:30:0x0074, B:31:0x001a, B:33:0x001e, B:36:0x0027, B:38:0x002d, B:39:0x0030), top: B:3:0x0002, outer: #0 }] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0074 A[Catch: all -> 0x00a2, Exception -> 0x00a4, TryCatch #1 {Exception -> 0x00a4, blocks: (B:4:0x0002, B:6:0x0007, B:8:0x000c, B:11:0x0012, B:13:0x0017, B:14:0x0032, B:16:0x0036, B:18:0x0043, B:20:0x0047, B:24:0x0061, B:26:0x0068, B:27:0x007f, B:30:0x0074, B:31:0x001a, B:33:0x001e, B:36:0x0027, B:38:0x002d, B:39:0x0030), top: B:3:0x0002, outer: #0 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private synchronized boolean startBoostInternal(com.miui.server.rtboost.SchedBoostService.BoostThreadInfo r8) {
        /*
            r7 = this;
            monitor-enter(r7)
            r0 = 0
            int r1 = r8.mode     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r2 = 1
            if (r1 == r2) goto L27
            int r1 = r8.mode     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r3 = 5
            if (r1 == r3) goto L27
            int r1 = r8.mode     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r3 = 4
            if (r1 != r3) goto L12
            goto L27
        L12:
            int r1 = r8.mode     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r3 = 6
            if (r1 != r3) goto L1a
            int[] r1 = r7.mGpuAndCpuPerfList     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            goto L32
        L1a:
            boolean r1 = com.miui.server.rtboost.SchedBoostService.DEBUG     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            if (r1 == 0) goto L25
            java.lang.String r1 = "SchedBoost"
            java.lang.String r2 = "current sched mode no need to boost"
            android.util.Slog.d(r1, r2)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
        L25:
            monitor-exit(r7)
            return r0
        L27:
            boolean r1 = r7.needBoostHigherFreq(r8)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            if (r1 == 0) goto L30
            int[] r1 = com.miui.server.rtboost.SchedBoostService.QCOM_GPU_AND_CPU_HIGHER_FREQ     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            goto L32
        L30:
            int[] r1 = r7.mDefultPerfList     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
        L32:
            int r3 = r7.mHandle     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            if (r3 == 0) goto L61
            long r3 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            long r5 = r7.mLastPerfAcqTime     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            long r3 = r3 - r5
            r5 = 300(0x12c, double:1.48E-321)
            int r5 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r5 >= 0) goto L61
            boolean r2 = com.miui.server.rtboost.SchedBoostService.DEBUG     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            if (r2 == 0) goto L5f
            java.lang.String r2 = "SchedBoost"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r5.<init>()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.String r6 = "last perfacq still work, skip, "
            java.lang.StringBuilder r5 = r5.append(r6)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.StringBuilder r5 = r5.append(r3)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.String r5 = r5.toString()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            android.util.Slog.d(r2, r5)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
        L5f:
            monitor-exit(r7)
            return r0
        L61:
            r7.stopBoostInternal()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            boolean r3 = com.miui.server.rtboost.SchedBoostService.IS_MTK_DEVICE     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            if (r3 == 0) goto L74
            android.perf.PerfMTKStub r3 = r7.mMTKPerfStub     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            long r4 = r8.duration     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            int r4 = (int) r4     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            int r3 = r3.perfLockAcquire(r4, r1)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r7.mHandle = r3     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            goto L7f
        L74:
            android.perf.PerfStub r3 = r7.mQcomPerfStub     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            long r4 = r8.duration     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            int r4 = (int) r4     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            int r3 = r3.perfLockAcquire(r4, r1)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r7.mHandle = r3     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
        L7f:
            long r3 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r7.mLastPerfAcqTime = r3     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.String r3 = "SchedBoost"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            r4.<init>()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.String r5 = "startBoostInternal "
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            long r5 = r8.duration     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            android.util.Slog.d(r3, r4)     // Catch: java.lang.Throwable -> La2 java.lang.Exception -> La4
            monitor-exit(r7)
            return r2
        La2:
            r8 = move-exception
            goto Lc3
        La4:
            r1 = move-exception
            java.lang.String r2 = "SchedBoost"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2
            r3.<init>()     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = "startBoostInternal exception "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> La2
            java.lang.StringBuilder r3 = r3.append(r1)     // Catch: java.lang.Throwable -> La2
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> La2
            android.util.Slog.e(r2, r3)     // Catch: java.lang.Throwable -> La2
            r1.printStackTrace()     // Catch: java.lang.Throwable -> La2
            monitor-exit(r7)
            return r0
        Lc3:
            monitor-exit(r7)
            throw r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.rtboost.SchedBoostService.startBoostInternal(com.miui.server.rtboost.SchedBoostService$BoostThreadInfo):boolean");
    }

    private synchronized boolean startBoostInternalWithOpcode(BoostThreadInfo threadInfo) {
        try {
            if (needBoostHigherFreq(threadInfo)) {
                threadInfo.mode = 10;
            }
            int opcode = MiuiProcess.schedCodeToOpcode(threadInfo.mode);
            if (opcode < 0) {
                return false;
            }
            if (this.mHandle != 0) {
                long lastPerfAcqDuration = SystemClock.uptimeMillis() - this.mLastPerfAcqTime;
                if (lastPerfAcqDuration < 300) {
                    if (DEBUG) {
                        Slog.d(TAG, "last perfacq still work, skip, " + lastPerfAcqDuration);
                    }
                    return false;
                }
            }
            stopBoostInternal();
            this.mHandle = this.mQcomPerfStub.perfHint(4512, threadInfo.procName, (int) threadInfo.duration, opcode);
            this.mLastPerfAcqTime = SystemClock.uptimeMillis();
            Slog.d(TAG, "startBoostInternalWithOpcode " + threadInfo.duration + " ms, opcode: " + opcode);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "startBoostInternalWithOpcode exception " + e);
            e.printStackTrace();
            return false;
        }
    }

    private synchronized void stopBoostInternal() {
        int i = this.mHandle;
        if (i == 0) {
            if (DEBUG) {
                Slog.w(TAG, "has released boost yet");
            }
            return;
        }
        try {
            if (IS_MTK_DEVICE) {
                this.mMTKPerfStub.perfLockReleaseHandler(i);
            } else {
                this.mQcomPerfStub.perfLockReleaseHandler(i);
            }
            this.mHandle = 0;
            Slog.d(TAG, "stopBoostInternal");
        } catch (Exception e) {
            Slog.e(TAG, "stopBoostInternal exception " + e);
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: changeSchedBoostPolicy, reason: merged with bridge method [inline-methods] */
    public void lambda$new$0(int temp) {
        if (!RealTimeModeControllerImpl.ENABLE_RT_MODE || !RealTimeModeControllerImpl.ENABLE_TEMP_LIMIT_ENABLE) {
            return;
        }
        if (temp >= RealTimeModeControllerImpl.RT_TEMPLIMIT_CEILING && !this.isNormalPolicy) {
            this.isNormalPolicy = true;
            Slog.d(TAG, String.format("onThermalTempChange, set sched policy OTHER", new Object[0]));
        } else if (temp <= RealTimeModeControllerImpl.RT_TEMPLIMIT_BOTTOM && this.isNormalPolicy) {
            this.isNormalPolicy = false;
            Slog.d(TAG, String.format("onThermalTempChange, set sched policy FIFO", new Object[0]));
        }
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public boolean checkThreadBoost(int tid) {
        if (this.mBoostingMap.getByTid(tid) != null) {
            return true;
        }
        return false;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void setThreadSavedPriority(int[] tid, int prio) {
        if (tid.length > 0) {
            for (int threadTid : tid) {
                BoostThreadInfo info = this.mBoostingMap.getByTid(threadTid);
                if (info != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "setThreadSavedPriority tid: " + threadTid + ", before: " + info.savedPriority + ", after: " + prio);
                    }
                    info.savedPriority = prio;
                }
            }
        }
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void stopCurrentSchedBoost() {
        synchronized (this.mBoostingMap.mMapLock) {
            ArrayList<BoostThreadInfo> list = this.mBoostingMap.getList();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    BoostThreadInfo threadInfo = list.get(i);
                    if (!this.mScreenOff && threadInfo.mode == 4) {
                        return;
                    }
                    this.mHandler.removeResetThreadsMsg(threadInfo);
                    this.mHandler.obtainResetSchedThreadsMsg(threadInfo).sendToTarget();
                }
            }
            this.mBoostingMap.clear();
        }
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void boostHomeAnim(long duration, int mode) {
        beginSchedThreads(new int[]{this.mHomeMainThreadId, this.mHomeRenderThreadId}, duration, "com.miui.home", mode);
    }

    public boolean needBoostHigherFreq(BoostThreadInfo threadInfo) {
        if (!IS_MTK_DEVICE) {
            int orientation = this.mContext.getResources().getConfiguration().orientation;
            if (orientation == 2 && threadInfo.mode == 4) {
                if (DEBUG) {
                    Slog.d(TAG, "needBoostHigherFreq");
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private void dump(PrintWriter pw, String[] args) {
        StringBuilder append = new StringBuilder().append("IS_MTK_DEVICE: ");
        boolean z = IS_MTK_DEVICE;
        pw.println(append.append(z).toString());
        if (z) {
            pw.println("ENABLE_RTMODE_UCLAMP: " + ENABLE_RTMODE_UCLAMP);
            pw.println("TASK_UCLAMP_MIN: " + this.TASK_UCLAMP_MIN);
            pw.println("TASK_UCLAMP_MIN: " + this.TASK_UCLAMP_MAX);
        }
        pw.println("mPreBoostProcessName: " + this.mPreBoostProcessName);
        pw.println("currently isNormalPolicy: " + this.isNormalPolicy);
        pw.println("AlwaysRtTids: ");
        synchronized (this.mAlwaysRtTids) {
            Iterator<Integer> it = this.mAlwaysRtTids.iterator();
            while (it.hasNext()) {
                int tid = it.next().intValue();
                pw.println("    " + tid);
            }
        }
        pw.println("Boosting Threads: ");
        synchronized (this.mBoostingMap.mMapLock) {
            Iterator it2 = this.mBoostingMap.infoList.iterator();
            while (it2.hasNext()) {
                BoostThreadInfo info = (BoostThreadInfo) it2.next();
                info.dump(pw, args);
            }
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("sched boost (SchedBoostService):");
            try {
                RealTimeModeControllerImpl.get();
                RealTimeModeControllerImpl.dump(pw, args);
                dump(pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new SchedBoostShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class SchedBoostShellCommand extends ShellCommand {
        SchedBoostService mService;

        SchedBoostShellCommand(SchedBoostService service) {
            this.mService = service;
        }

        public int onCommand(String cmd) {
            FileDescriptor fd = getOutFileDescriptor();
            PrintWriter pw = getOutPrintWriter();
            String[] args = getAllArgs();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                this.mService.dump(fd, pw, args);
                return -1;
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("sched boost (SchedBoostService) commands:");
            pw.println();
        }
    }
}
