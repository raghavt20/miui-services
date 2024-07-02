package com.android.server.am;

import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.BatteryManagerInternal;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.app.IProcessProphet;
import com.android.internal.app.ProcessProphetInfo;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import com.android.server.am.ProcessProphetModel;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.security.AccessControlImpl;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class ProcessProphetImpl implements ProcessProphetStub {
    private static final long COPY_INTERVAL = 2000;
    private static final String FLAG_PRELOAD = "ProcessProphet";
    private static final int MSG_AUDIO_FOCUS_CHANGED = 4;
    private static final int MSG_BT_CONNECTED = 2;
    private static final int MSG_BT_DISCONNECTED = 3;
    private static final int MSG_COPY = 6;
    private static final int MSG_DUMP = 13;
    private static final int MSG_IDLE_UPDATE = 11;
    private static final int MSG_KILL_PROC = 10;
    private static final int MSG_LAUNCH_EVENT = 5;
    private static final int MSG_MEM_PRESSURE = 12;
    private static final int MSG_MTBF_TEST = 14;
    private static final int MSG_PROC_CHECK = 9;
    private static final int MSG_PROC_DIED = 8;
    private static final int MSG_PROC_START = 7;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 15;
    private static final int MSG_UNLOCK = 1;
    private static final long PREDICTION_INTERVAL = 120000;
    private static final String TAG = "ProcessProphet";
    private static final String WHITELIST_DEFAULT_PATH = "/product/etc/procprophet.xml";
    private static long MEM_THRESHOLD = 614400;
    private static long EMPTY_PROCS_PSS_THRESHOLD = 512000;
    private static int TRACK_INTERVAL_TIME = 1;
    private static final int[] mEmptyThreslist = {0, 500, 600, ScreenRotationAnimationImpl.COVER_EGE};
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.procprophet.debug", false);
    private static HashSet<String> sWhiteListEmptyProc = new HashSet<>();
    private static HashSet<String> sBlackListLaunch = new HashSet<>();
    private static HashSet<String> sBlackListBTAudio = new HashSet<>();
    private boolean mEnable = SystemProperties.getBoolean("persist.sys.procprophet.enable", false);
    private ActivityManagerService mAMS = null;
    private ActivityTaskManagerService mATMS = null;
    private ProcessManagerService mPMS = null;
    private Context mContext = null;
    public MyHandler mHandler = null;
    private HandlerThread mHandlerThread = null;
    private ProcessProphetModel mProcessProphetModel = null;
    private ProcessProphetCloud mProcessProphetCloud = null;
    private boolean afterFirstUnlock = false;
    private boolean mBTConnected = false;
    private boolean mInitialized = false;
    private long mLastEmptyProcStartUpTime = 0;
    private long mLastCopyUpTime = 0;
    private final Object mModelTrackLock = new Object();
    private long mLastReportTime = 0;
    private long mStartedProcs = 0;
    private long mHits = 0;
    private long mKilledProcs = 0;
    private long mAMKilledProcs = 0;
    private long mNativeKilledProcs = 0;
    private long[] mModelTrackList = new long[6];
    private ArrayList<Double> mModelPredList = new ArrayList<>(Collections.nCopies(6, Double.valueOf(0.0d)));
    private String mLastLaunchedPkg = "";
    private String mLastAudioFocusPkg = "";
    public HashMap<String, ProcessRecord> mAliveEmptyProcs = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessProphetImpl> {

        /* compiled from: ProcessProphetImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessProphetImpl INSTANCE = new ProcessProphetImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ProcessProphetImpl m663provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ProcessProphetImpl m662provideNewInstance() {
            return new ProcessProphetImpl();
        }
    }

    public void init(Context context, ActivityManagerService ams) {
        if (!this.mEnable || this.mInitialized) {
            return;
        }
        long startUpTime = SystemClock.uptimeMillis();
        HandlerThread handlerThread = new HandlerThread("ProcessProphet");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
        this.mContext = context;
        this.mAMS = ams;
        this.mATMS = ActivityTaskManager.getService();
        this.mPMS = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        ServiceManager.addService("procprophet", new BinderService());
        initThreshold();
        if (this.mAMS == null || this.mATMS == null || this.mPMS == null) {
            this.mEnable = false;
            this.mInitialized = false;
            Slog.e("ProcessProphet", "disable ProcessProphet for dependencies service not available");
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessProphetImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    ProcessProphetImpl.this.handlePostInit();
                }
            });
            Slog.d("ProcessProphet", "pre init consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePostInit() {
        long startUpTime = SystemClock.uptimeMillis();
        IntentFilter filter = new IntentFilter("android.intent.action.USER_PRESENT");
        filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        this.mContext.registerReceiver(new MyReceiver(), filter);
        this.mProcessProphetModel = new ProcessProphetModel();
        ProcessProphetJobService.schedule(this.mContext);
        getWhitePackages();
        final ClipboardManager clipboardManager = (ClipboardManager) this.mContext.getSystemService("clipboard");
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() { // from class: com.android.server.am.ProcessProphetImpl.2
            @Override // android.content.ClipboardManager.OnPrimaryClipChangedListener
            public void onPrimaryClipChanged() {
                ClipData clipData;
                CharSequence text;
                if (ProcessProphetImpl.this.isEnable() && SystemClock.uptimeMillis() - ProcessProphetImpl.this.mLastCopyUpTime >= ProcessProphetImpl.COPY_INTERVAL) {
                    ProcessProphetImpl.this.mLastCopyUpTime = SystemClock.uptimeMillis();
                    if (clipboardManager.hasPrimaryClip() && (clipData = clipboardManager.getPrimaryClip()) != null && clipData.getItemCount() > 0 && (text = clipData.getItemAt(0).getText()) != null) {
                        Message msg = ProcessProphetImpl.this.mHandler.obtainMessage(6, text);
                        ProcessProphetImpl.this.mHandler.sendMessage(msg);
                    }
                }
            }
        });
        ProcessProphetCloud processProphetCloud = new ProcessProphetCloud();
        this.mProcessProphetCloud = processProphetCloud;
        processProphetCloud.initCloud(this, this.mProcessProphetModel, this.mContext);
        Message msgRegisterCloud = this.mHandler.obtainMessage(15);
        this.mHandler.sendMessage(msgRegisterCloud);
        this.mInitialized = true;
        Slog.d("ProcessProphet", "post init consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
    }

    public void updateEnable(boolean newEnable) {
        this.mEnable = newEnable;
        if (!newEnable) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(6);
            this.mHandler.removeMessages(7);
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", "mEnable change to" + newEnable + "complete.");
        }
    }

    public void updateTrackInterval(int intervalT) {
        if (DEBUG) {
            Slog.i("ProcessProphet", "TRACK_INTERVAL_TIME" + TRACK_INTERVAL_TIME + "change to" + intervalT + "complete.");
        }
        TRACK_INTERVAL_TIME = intervalT;
    }

    public void updateImplThreshold(long newThres, String updateName) {
        if (updateName.equals("memPress")) {
            MEM_THRESHOLD = FormatBytesUtil.KB * newThres;
        } else if (updateName.equals("emptyMem")) {
            EMPTY_PROCS_PSS_THRESHOLD = FormatBytesUtil.KB * newThres;
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", updateName + " change to " + newThres + " complete.");
        }
    }

    public void updateList(String[] newList, String updateName) {
        int i = 0;
        if (updateName.equals("whitelist")) {
            int length = newList.length;
            while (i < length) {
                String str = newList[i];
                if (sWhiteListEmptyProc.contains(str)) {
                    sWhiteListEmptyProc.remove(str);
                }
                i++;
            }
        } else if (updateName.equals("blakclist")) {
            int length2 = newList.length;
            while (i < length2) {
                String str2 = newList[i];
                if (!sBlackListLaunch.contains(str2)) {
                    sBlackListLaunch.add(str2);
                }
                i++;
            }
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", updateName + " change complete.");
        }
    }

    public int getRamLayerIndex() {
        int index;
        int memory_GB = (int) ((Process.getTotalMemory() / FormatBytesUtil.GB) + 1);
        if (memory_GB <= 4) {
            index = 0;
        } else {
            index = (memory_GB / 2) - 2;
            if (index >= 3) {
                index = 3;
            }
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", "Total mem is " + memory_GB + " index is " + index);
        }
        return index;
    }

    private void initThreshold() {
        try {
            MEM_THRESHOLD = SystemProperties.getInt("persist.sys.procprophet.mem_threshold", 600) * FormatBytesUtil.KB;
            EMPTY_PROCS_PSS_THRESHOLD = SystemProperties.getInt("persist.sys.procprophet.max_pss", mEmptyThreslist[getRamLayerIndex()]) * FormatBytesUtil.KB;
            if (DEBUG) {
                Slog.i("ProcessProphet", "Init threshold: mem threshold is " + MEM_THRESHOLD + ". Empty Process PSS Threshold is " + EMPTY_PROCS_PSS_THRESHOLD);
            }
        } catch (Exception e) {
            Slog.e("ProcessProphet", "init mem threshold failure!");
        }
    }

    public boolean isEnable() {
        return this.mEnable && this.mInitialized && this.afterFirstUnlock;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:5:0x0023. Please report as an issue. */
    /* JADX WARN: Finally extract failed */
    public void getWhitePackages() {
        long startUpTime = SystemClock.uptimeMillis();
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(WHITELIST_DEFAULT_PATH);
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStream, "utf-8");
                for (int event = xmlParser.getEventType(); event != 1; event = xmlParser.next()) {
                    switch (event) {
                        case 0:
                        case 1:
                        default:
                        case 2:
                            if ("white-emptyproc".equals(xmlParser.getName())) {
                                sWhiteListEmptyProc.add(xmlParser.getAttributeValue(0));
                            } else if ("black-launch".equals(xmlParser.getName())) {
                                sBlackListLaunch.add(xmlParser.getAttributeValue(0));
                            } else if ("black-bt".equals(xmlParser.getName())) {
                                sBlackListBTAudio.add(xmlParser.getAttributeValue(0));
                            }
                        case 3:
                    }
                }
                try {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Slog.e("ProcessProphet", e.getMessage());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            } catch (IOException | XmlPullParserException e2) {
                Slog.e("ProcessProphet", e2.getMessage());
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e3) {
                            Slog.e("ProcessProphet", e3.getMessage());
                        }
                    }
                } catch (Throwable th2) {
                    throw th2;
                }
            }
            if (DEBUG) {
                String whiteListDump = "whiteList: " + sWhiteListEmptyProc.size();
                Iterator<String> it = sWhiteListEmptyProc.iterator();
                while (true) {
                    if (it.hasNext()) {
                        String procName = it.next();
                        whiteListDump = whiteListDump + " " + procName + ",";
                        if (whiteListDump.length() > 200) {
                            whiteListDump = whiteListDump.substring(0, whiteListDump.length() - 1) + "...... ";
                        }
                    }
                }
                Slog.d("ProcessProphet", whiteListDump.substring(0, whiteListDump.length() - 1));
                String blackListLaunch = "blackListLaunch: " + sBlackListLaunch.size();
                Iterator<String> it2 = sBlackListLaunch.iterator();
                while (true) {
                    if (it2.hasNext()) {
                        String procName2 = it2.next();
                        blackListLaunch = blackListLaunch + " " + procName2 + ",";
                        if (blackListLaunch.length() > 200) {
                            blackListLaunch = blackListLaunch.substring(0, blackListLaunch.length() - 1) + "...... ";
                        }
                    }
                }
                Slog.d("ProcessProphet", blackListLaunch.substring(0, blackListLaunch.length() - 1));
                String blackListBT = "blackListBT: " + sBlackListBTAudio.size();
                Iterator<String> it3 = sBlackListBTAudio.iterator();
                while (true) {
                    if (it3.hasNext()) {
                        String procName3 = it3.next();
                        blackListBT = blackListBT + " " + procName3 + ",";
                        if (blackListBT.length() > 200) {
                            blackListBT = blackListBT.substring(0, blackListBT.length() - 1) + "...... ";
                        }
                    }
                }
                Slog.d("ProcessProphet", blackListBT.substring(0, blackListBT.length() - 1));
            }
            Slog.d("ProcessProphet", "getWhitePackages consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
        } catch (Throwable th3) {
            if (inputStream == null) {
                throw th3;
            }
            try {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    Slog.e("ProcessProphet", e4.getMessage());
                    throw th3;
                }
                throw th3;
            } catch (Throwable th4) {
                throw th4;
            }
        }
    }

    public void notifyAudioFocusChanged(String callingPackageName) {
        if (!isEnable()) {
            return;
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", "Getting audio focus: " + callingPackageName);
        }
        Message msg = this.mHandler.obtainMessage(4, callingPackageName);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAudioFocusChanged(String callingPackageName) {
        if (!this.mBTConnected || this.mLastAudioFocusPkg.equals(callingPackageName) || sBlackListBTAudio.contains(callingPackageName)) {
            return;
        }
        Slog.i("ProcessProphet", "update audio focus changed to " + callingPackageName);
        this.mProcessProphetModel.updateBTAudioEvent(callingPackageName);
        this.mLastAudioFocusPkg = callingPackageName;
    }

    public void reportLaunchEvent(String packageName, int launchType, int launchDurationMs) {
        if (isEnable()) {
            Message msg = this.mHandler.obtainMessage(5, launchType, launchDurationMs, packageName);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLaunchEvent(String packageName, int launchType, int launchDurationMs) {
        if (this.mLastLaunchedPkg.equals(packageName)) {
            return;
        }
        this.mLastLaunchedPkg = packageName;
        if (sBlackListLaunch.contains(packageName)) {
            return;
        }
        this.mProcessProphetModel.updateLaunchEvent(packageName);
        if (DEBUG) {
            Slog.d("ProcessProphet", packageName + " Launched, type=" + launchType + ", duration=" + launchDurationMs);
        }
        synchronized (this.mAliveEmptyProcs) {
            if (this.mAliveEmptyProcs.containsKey(packageName)) {
                this.mHits++;
                this.mAliveEmptyProcs.remove(packageName);
                Slog.i("ProcessProphet", "AliveEmptyProcs remove: " + packageName + " as launched.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnlock() {
        if (!this.afterFirstUnlock) {
            if (DEBUG) {
                Slog.d("ProcessProphet", "skip first unlock.");
            }
            this.afterFirstUnlock = true;
            return;
        }
        triggerPrediction();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBluetoothConnected(String deviceName) {
        this.mBTConnected = true;
        this.mProcessProphetModel.notifyBTConnected();
        triggerPrediction();
        Slog.i("ProcessProphet", "Bluetooth Connected, device = " + deviceName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBluetoothDisConnected() {
        this.mBTConnected = false;
        this.mLastAudioFocusPkg = "";
        Slog.i("ProcessProphet", "Bluetooth DisConnected.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCopy(CharSequence text) {
        if (DEBUG) {
            Slog.d("ProcessProphet", "copied: " + ((Object) text));
        }
        if (this.mHandler.hasMessages(6)) {
            Slog.i("ProcessProphet", "copy skip...");
            return;
        }
        try {
            ActivityTaskManager.RootTaskInfo info = this.mATMS.getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null) {
                String curTopProcName = info.topActivity.getPackageName();
                String matchedPkgName = this.mProcessProphetModel.updateCopyEvent(text, curTopProcName);
                if (matchedPkgName != null) {
                    triggerPrediction();
                }
                return;
            }
            Slog.e("ProcessProphet", "getFocusedRootTaskInfo error.");
        } catch (RemoteException e) {
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:7:0x002a */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void triggerPrediction() {
        /*
            Method dump skipped, instructions count: 279
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ProcessProphetImpl.triggerPrediction():void");
    }

    private boolean allowToStartNewProc() {
        if (!DEBUG) {
            long uptimeDiff = SystemClock.uptimeMillis() - this.mLastEmptyProcStartUpTime;
            if (uptimeDiff < PREDICTION_INTERVAL) {
                return false;
            }
        }
        int batteryLevel = ((BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class)).getBatteryLevel();
        if (batteryLevel < 20) {
            if (DEBUG) {
                Slog.d("ProcessProphet", "batteryLevel low: " + batteryLevel);
            }
            return false;
        }
        if (estimateMemPressure()) {
            if (DEBUG) {
                Slog.d("ProcessProphet", "terminate proc start by mem pressure.");
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean tryToStartEmptyProc(String processName) {
        if (processName == null || processName.equals("") || !sWhiteListEmptyProc.contains(processName)) {
            return false;
        }
        Slog.d("ProcessProphet", "trying to start proc: " + processName);
        return startEmptyProc(processName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean startEmptyProc(String processName) {
        if (processName == null || processName.equals("")) {
            return false;
        }
        try {
            ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(processName, FormatBytesUtil.KB, 0);
            if (info == null) {
                return false;
            }
            synchronized (this.mAMS) {
                ProcessRecord newApp = this.mAMS.startProcessLocked(processName, info, false, 0, new HostingRecord("ProcessProphet", processName), 0, false, false, "android");
                if (newApp == null) {
                    Slog.w("ProcessProphet", "preload " + processName + " failed!");
                    return false;
                }
                if (newApp.getPid() > 0) {
                    Slog.i("ProcessProphet", "preload " + processName + " existed!");
                    return false;
                }
                this.mLastEmptyProcStartUpTime = SystemClock.uptimeMillis();
                return true;
            }
        } catch (RemoteException e) {
            Slog.w("ProcessProphet", "error in getApplicationInfo!" + e);
            return false;
        }
    }

    private void tryToKillProc(ProcessRecord app) {
        Message msg = this.mHandler.obtainMessage(10, app);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKillProc(ProcessRecord app) {
        if (app == null) {
            return;
        }
        ProcessStateRecord state = app.mState;
        int curAdj = state.getCurAdj();
        int curProcState = state.getCurProcState();
        if (DEBUG) {
            Slog.i("ProcessProphet", "trying to kill: " + app.processName + "(" + app.mPid + ") curAdj=" + curAdj + " curProcState=" + curProcState + " adjType=" + state.getAdjType() + " isKilled=" + (app.isKilled() ? "true" : "false") + " isKilledByAm=" + (app.isKilledByAm() ? "true" : "false"));
        }
        if (app.mPid > 0 && !app.isKilledByAm() && !app.isKilled() && curProcState == 19 && curAdj == 800) {
            Slog.d("ProcessProphet", "killing proc: " + app);
            this.mPMS.getProcessKiller().killApplication(app, "pp-oom", false);
        }
    }

    public void reportProcStarted(ProcessRecord app, int pid) {
        if (isPredictedProc(app)) {
            Message msg = this.mHandler.obtainMessage(7, pid, 0, app);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleProcStarted(ProcessRecord app, int pid) {
        Slog.i("ProcessProphet", "preload " + app.processName + " success! pid:" + pid + " uid:" + app.uid);
        synchronized (this.mAliveEmptyProcs) {
            this.mAliveEmptyProcs.put(app.processName, app);
        }
        this.mStartedProcs++;
        if (DEBUG) {
            Slog.i("ProcessProphet", "add: " + app);
        }
        this.mHandler.removeMessages(9);
        Message msg = this.mHandler.obtainMessage(9, 3, 0, null);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCheckEmptyProcs(int cycleTime) {
        ProcessRecord app;
        Long pss;
        long startUpTime = SystemClock.uptimeMillis();
        ArrayList<ProcessProphetModel.PkgValuePair> sortedList = this.mProcessProphetModel.sortEmptyProcs(this.mAliveEmptyProcs);
        long totalPss = 0;
        Iterator<ProcessProphetModel.PkgValuePair> it = sortedList.iterator();
        while (it.hasNext()) {
            ProcessProphetModel.PkgValuePair p = it.next();
            if (p != null && p.pkgName != null) {
                synchronized (this.mAliveEmptyProcs) {
                    app = this.mAliveEmptyProcs.get(p.pkgName);
                }
                if (app != null && (pss = getPssByUid(app.processName, app.info.uid)) != null && pss.longValue() > 0) {
                    this.mProcessProphetModel.updatePssInRecord(p.pkgName, pss);
                    if (pss.longValue() + totalPss > EMPTY_PROCS_PSS_THRESHOLD) {
                        tryToKillProc(app);
                    } else {
                        totalPss += pss.longValue();
                    }
                }
            }
        }
        if (cycleTime - 1 > 0 && sortedList.size() > 0) {
            Message msg = this.mHandler.obtainMessage(9, cycleTime - 1, 0, null);
            this.mHandler.sendMessageDelayed(msg, 10000L);
        }
        if (DEBUG) {
            Slog.d("ProcessProphet", "handleCheckEmptyProcs consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
        }
    }

    public void reportProcDied(ProcessRecord app) {
        if (isEnable()) {
            synchronized (this.mAliveEmptyProcs) {
                if (this.mAliveEmptyProcs.containsKey(app.processName)) {
                    if (app.isKilledByAm()) {
                        this.mAMKilledProcs++;
                        Slog.i("ProcessProphet", app.processName + " is killed by am.");
                    } else {
                        this.mNativeKilledProcs++;
                        Slog.i("ProcessProphet", app.processName + " is killed by native.");
                    }
                    this.mKilledProcs++;
                    this.mAliveEmptyProcs.remove(app.processName);
                    if (DEBUG) {
                        Slog.i("ProcessProphet", "remove: " + app.processName + " as died.");
                    }
                }
            }
        }
    }

    public boolean isPredictedProc(ProcessRecord app) {
        if (isEnable() && app != null) {
            try {
                if (app.getHostingRecord() != null) {
                    return "ProcessProphet".equals(app.getHostingRecord().getType());
                }
            } catch (Exception e) {
                Slog.e("ProcessProphet", "Fail to check hostrecord type." + e);
                return false;
            }
        }
        return false;
    }

    public boolean isNeedProtect(ProcessRecord app) {
        if (!isPredictedProc(app)) {
            return false;
        }
        String processName = app.processName;
        synchronized (this.mAliveEmptyProcs) {
            if (!this.mAliveEmptyProcs.containsKey(processName)) {
                return false;
            }
            ProcessStateRecord state = app.mState;
            int curAdj = state.getCurAdj();
            int curProcState = state.getCurProcState();
            if (DEBUG && curProcState != 19 && curProcState != 10 && curProcState != 11 && curProcState != 20) {
                Slog.d("ProcessProphet", "processName = " + processName + " curAdj=" + curAdj + " curProcState=" + curProcState + " adjType=" + state.getAdjType() + " isKilled=" + (app.isKilled() ? "true" : "false") + " isKilledByAm=" + (app.isKilledByAm() ? "true" : "false"));
            }
            return app.mPid > 0 && !app.isKilledByAm() && !app.isKilled() && curProcState == 19 && curAdj >= 800;
        }
    }

    public void notifyIdleUpdate() {
        if (isEnable()) {
            Message msg = this.mHandler.obtainMessage(11);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleIdleUpdate() {
        Slog.i("ProcessProphet", "IUpdate.");
        this.mProcessProphetModel.conclude();
        synchronized (this.mModelTrackLock) {
            this.mModelTrackList = this.mProcessProphetModel.updateModelSizeTrack();
            ArrayList<Double> tmp = this.mProcessProphetModel.uploadModelPredProb();
            for (int i = 0; i < this.mModelPredList.size(); i++) {
                ArrayList<Double> arrayList = this.mModelPredList;
                arrayList.set(i, Double.valueOf(arrayList.get(i).doubleValue() + tmp.get(i).doubleValue()));
            }
        }
    }

    public void reportMemPressure(int newPressureState) {
        if (!isEnable()) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(12, newPressureState, 0);
        this.mHandler.sendMessageAtFrontOfQueue(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMemPressure(int newPressureState) {
        if (estimateMemPressure()) {
            Slog.i("ProcessProphet", "trying to kill all empty procs.");
            synchronized (this.mAliveEmptyProcs) {
                for (ProcessRecord app : this.mAliveEmptyProcs.values()) {
                    if (app != null) {
                        tryToKillProc(app);
                    }
                }
            }
        }
    }

    private boolean estimateMemPressure() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long[] rawInfo = minfo.getRawInfo();
        long otherFile = rawInfo[3] + rawInfo[26] + rawInfo[2];
        long needRemovedFile = rawInfo[4] + rawInfo[18] + rawInfo[26];
        if (otherFile > needRemovedFile) {
            otherFile -= needRemovedFile;
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", "Other File: " + otherFile + "KB.");
        }
        if (otherFile < MEM_THRESHOLD) {
            Slog.i("ProcessProphet", "mem pressure is critical.");
            return true;
        }
        return false;
    }

    private Long getPssByUid(String pkgName, int uid) {
        List<ProcessRecord> appList;
        long[] jArr = null;
        if (pkgName == null || pkgName.equals("") || uid <= 0) {
            return null;
        }
        long startUpTime = SystemClock.uptimeMillis();
        List<ProcessRecord> appList2 = this.mPMS.getProcessRecordListByPackageAndUid(pkgName, uid);
        if (DEBUG) {
            Slog.i("ProcessProphet", "get procs by uid consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
        }
        long totalPss = 0;
        for (ProcessRecord app : appList2) {
            long startUpTimeGetPss = SystemClock.uptimeMillis();
            Long dPss = Long.valueOf(Debug.getPss(app.mPid, jArr, jArr));
            totalPss += dPss.longValue();
            if (!DEBUG) {
                appList = appList2;
            } else {
                appList = appList2;
                Slog.i("ProcessProphet", "\tEmptyProcs " + app.processName + "(" + app.mPid + "): Pss=" + (dPss.longValue() / FormatBytesUtil.KB) + "MB, querying consumed " + (SystemClock.uptimeMillis() - startUpTimeGetPss) + "ms");
            }
            jArr = null;
            appList2 = appList;
        }
        if (DEBUG) {
            Slog.i("ProcessProphet", "Total pss of " + pkgName + " is " + (totalPss / FormatBytesUtil.KB) + "MB, total consumed " + (SystemClock.uptimeMillis() - startUpTime) + "ms");
        }
        return Long.valueOf(totalPss);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDump() {
        this.mProcessProphetModel.dump();
        Slog.i("ProcessProphet", "Dumping AliveEmptyProcs:");
        synchronized (this.mAliveEmptyProcs) {
            for (ProcessRecord app : this.mAliveEmptyProcs.values()) {
                if (app.mPid > 0) {
                    long dPss = Debug.getPss(app.mPid, null, null);
                    Slog.i("ProcessProphet", "\tPss=" + (dPss / FormatBytesUtil.KB) + "MB " + app);
                }
            }
        }
    }

    public void testLU(String targetProcName) {
        if (targetProcName == null || targetProcName.equals("")) {
            Slog.d("ProcessProphet", "target proc name error!");
        } else {
            Slog.d("ProcessProphet", "enter LU test mode, target: " + targetProcName);
        }
        this.mProcessProphetModel.reset();
        for (int i = 0; i < 5; i++) {
            Message msg1 = this.mHandler.obtainMessage(5, 0, i * 20, targetProcName);
            this.mHandler.sendMessage(msg1);
            Message msg2 = this.mHandler.obtainMessage(5, 0, i * 20, "com.miui.home");
            this.mHandler.sendMessage(msg2);
        }
        notifyIdleUpdate();
    }

    public void testBT(String targetProcName) {
        if (targetProcName == null || targetProcName.equals("")) {
            Slog.d("ProcessProphet", "target proc name error!");
        } else {
            Slog.d("ProcessProphet", "enter BT test mode, target: " + targetProcName);
        }
        this.mProcessProphetModel.reset();
        for (int i = 0; i < 5; i++) {
            MyHandler myHandler = this.mHandler;
            myHandler.sendMessage(myHandler.obtainMessage(2, "testBT"));
            MyHandler myHandler2 = this.mHandler;
            myHandler2.sendMessage(myHandler2.obtainMessage(4, targetProcName));
            MyHandler myHandler3 = this.mHandler;
            myHandler3.sendMessage(myHandler3.obtainMessage(3));
        }
        notifyIdleUpdate();
    }

    public void testMTBF() {
        if (isEnable()) {
            this.mProcessProphetModel.testMTBF();
            int randomNum = new Random().nextInt(10);
            if (randomNum < 3) {
                Slog.d("ProcessProphet", "MTBF testing:" + randomNum + " Prediction");
                triggerPrediction();
            } else if (randomNum < 6) {
                Slog.d("ProcessProphet", "MTBF testing:" + randomNum + " BT connection");
                Message msg = this.mHandler.obtainMessage(2, "testMTBF");
                this.mHandler.sendMessage(msg);
            } else if (randomNum < 8) {
                Slog.d("ProcessProphet", "MTBF testing:" + randomNum + " copy v.douyin.com");
                Message msg2 = this.mHandler.obtainMessage(6, "v.douyin.com");
                this.mHandler.sendMessage(msg2);
            } else {
                Slog.d("ProcessProphet", "MTBF testing:" + randomNum + " copy m.tb.cn");
                Message msg3 = this.mHandler.obtainMessage(6, "m.tb.cn");
                this.mHandler.sendMessage(msg3);
            }
            Message msg4 = this.mHandler.obtainMessage(14);
            this.mHandler.sendMessageDelayed(msg4, randomNum * AccessControlImpl.LOCK_TIME_OUT);
            Slog.d("ProcessProphet", "MTBF testing: next event after " + randomNum + " mins");
        }
    }

    /* loaded from: classes.dex */
    public class MyReceiver extends BroadcastReceiver {
        public MyReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.USER_PRESENT")) {
                ProcessProphetImpl.this.mHandler.sendMessage(ProcessProphetImpl.this.mHandler.obtainMessage(1));
                if (ProcessProphetImpl.DEBUG) {
                    Slog.d("ProcessProphet", "user present.");
                    return;
                }
                return;
            }
            if (action.equals("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                if (state == 2) {
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    String deviceName = device.getName();
                    Message msg = ProcessProphetImpl.this.mHandler.obtainMessage(2, deviceName);
                    ProcessProphetImpl.this.mHandler.sendMessage(msg);
                    return;
                }
                if (state == 0) {
                    Message msg2 = ProcessProphetImpl.this.mHandler.obtainMessage(3);
                    ProcessProphetImpl.this.mHandler.sendMessage(msg2);
                }
            }
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
                    ProcessProphetImpl.this.handleUnlock();
                    return;
                case 2:
                    ProcessProphetImpl.this.handleBluetoothConnected((String) msg.obj);
                    return;
                case 3:
                    ProcessProphetImpl.this.handleBluetoothDisConnected();
                    return;
                case 4:
                    ProcessProphetImpl.this.handleAudioFocusChanged((String) msg.obj);
                    return;
                case 5:
                    ProcessProphetImpl.this.handleLaunchEvent((String) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 6:
                    ProcessProphetImpl.this.handleCopy((CharSequence) msg.obj);
                    return;
                case 7:
                    ProcessProphetImpl.this.handleProcStarted((ProcessRecord) msg.obj, msg.arg1);
                    return;
                case 8:
                default:
                    return;
                case 9:
                    ProcessProphetImpl.this.handleCheckEmptyProcs(msg.arg1);
                    return;
                case 10:
                    ProcessProphetImpl.this.handleKillProc((ProcessRecord) msg.obj);
                    return;
                case 11:
                    ProcessProphetImpl.this.handleIdleUpdate();
                    return;
                case 12:
                    ProcessProphetImpl.this.handleMemPressure(msg.arg1);
                    return;
                case 13:
                    ProcessProphetImpl.this.handleDump();
                    return;
                case 14:
                    ProcessProphetImpl.this.testMTBF();
                    return;
                case 15:
                    ProcessProphetImpl.this.mProcessProphetCloud.registerProcProphetCloudObserver();
                    ProcessProphetImpl.this.mProcessProphetCloud.updateProcProphetCloudControlParas();
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IProcessProphet.Stub {
        private BinderService() {
        }

        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new ProcessProphetShellCmd().exec(this, in, out, err, args, callback, resultReceiver);
        }

        public ProcessProphetInfo getUploadData() throws RemoteException {
            return ProcessProphetImpl.this.getUploadData();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ProcessProphetInfo getUploadData() {
        long j;
        long j2;
        long j3;
        long j4;
        if (!isEnable()) {
            return null;
        }
        long curTime = SystemClock.elapsedRealtime();
        ProcessProphetInfo procProphetInfo = null;
        if (curTime - this.mLastReportTime >= TRACK_INTERVAL_TIME * 86400000) {
            synchronized (this.mModelTrackLock) {
                try {
                    j = this.mStartedProcs;
                    j2 = this.mHits;
                    j3 = this.mKilledProcs;
                    j4 = this.mAMKilledProcs;
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    long curTime2 = this.mNativeKilledProcs;
                    long[] jArr = this.mModelTrackList;
                    procProphetInfo = new ProcessProphetInfo(j, j2, j3, j4, curTime2, jArr[0], jArr[1], jArr[2], jArr[3], jArr[4], jArr[5], this.mModelPredList.get(0).doubleValue(), this.mModelPredList.get(1).doubleValue(), this.mModelPredList.get(2).doubleValue(), this.mModelPredList.get(3).doubleValue(), this.mModelPredList.get(4).doubleValue(), this.mModelPredList.get(5).doubleValue());
                    this.mLastReportTime = curTime;
                    if (DEBUG) {
                        Slog.i("ProcessProphet", "get onetrack records by getuploadData");
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        return procProphetInfo;
    }

    /* loaded from: classes.dex */
    private class ProcessProphetShellCmd extends ShellCommand {
        private ProcessProphetShellCmd() {
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
                    case -1548794112:
                        if (cmd.equals("force-start")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1276242363:
                        if (cmd.equals("pressure")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1147015235:
                        if (cmd.equals("testMTBF")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case -877170588:
                        if (cmd.equals("testBT")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case -877170561:
                        if (cmd.equals("testCP")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case -877170277:
                        if (cmd.equals("testLU")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3034545:
                        if (cmd.equals("bton")) {
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
                    case 3227604:
                        if (cmd.equals("idle")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 94070749:
                        if (cmd.equals("btoff")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 109757538:
                        if (cmd.equals("start")) {
                            c = 1;
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
                Slog.e("ShellCommand", "Error running shell command! " + e);
            }
            switch (c) {
                case 0:
                    ProcessProphetImpl.this.mHandler.sendMessage(ProcessProphetImpl.this.mHandler.obtainMessage(13));
                    return 0;
                case 1:
                    ProcessProphetImpl.this.tryToStartEmptyProc(peekNextArg());
                    return 0;
                case 2:
                    ProcessProphetImpl.this.startEmptyProc(peekNextArg());
                    return 0;
                case 3:
                    ProcessProphetImpl.this.notifyIdleUpdate();
                    return 0;
                case 4:
                    Message msg = ProcessProphetImpl.this.mHandler.obtainMessage(2, "cmd");
                    ProcessProphetImpl.this.mHandler.sendMessage(msg);
                    return 0;
                case 5:
                    Message msg2 = ProcessProphetImpl.this.mHandler.obtainMessage(3);
                    ProcessProphetImpl.this.mHandler.sendMessage(msg2);
                    return 0;
                case 6:
                    ProcessProphetImpl.this.reportMemPressure(4);
                    return 0;
                case 7:
                    if (ProcessProphetImpl.this.isEnable()) {
                        ProcessProphetImpl.this.testLU(peekNextArg());
                    }
                    return 0;
                case '\b':
                    if (ProcessProphetImpl.this.isEnable()) {
                        ProcessProphetImpl.this.testBT(peekNextArg());
                    }
                    return 0;
                case '\t':
                    if (ProcessProphetImpl.this.isEnable()) {
                        ProcessProphetImpl.this.mProcessProphetModel.testCP(peekNextArg());
                    }
                    return 0;
                case '\n':
                    ProcessProphetImpl.this.testMTBF();
                    return 0;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Process Prophet commands:");
        }
    }
}
