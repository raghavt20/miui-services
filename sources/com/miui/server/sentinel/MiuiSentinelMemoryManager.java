package com.miui.server.sentinel;

import android.content.Context;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.am.ProcessUtils;
import com.android.server.am.ScoutMemoryError;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import com.miui.server.AccessController;
import com.xiaomi.abtest.d.d;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.ExceptionEvent;
import miui.mqsas.sdk.event.GeneralExceptionEvent;

/* loaded from: classes.dex */
public class MiuiSentinelMemoryManager {
    private static final int DETAILS_LIMIT = 2000;
    private static final int END_TRACK = 4;
    private static final int END_TRACK_SIGNAL = 51;
    private static final String HANDLER_NAME = "sentineMemoryWork";
    private static final int MAX_FD_AMOUNT = 1000;
    private static final int MAX_THREAD_AMOUNT = 500;
    private static final String MEMLEAK_DIR = "/data/miuilog/stability/memleak/heapleak";
    private static final int REPORT_FD_AMOUNT_LEAKTOMQS = 9;
    private static final int REPORT_JAVAHEAP_LEAKTOMQS = 7;
    private static final int REPORT_NATIVEHEAP_LEAKTOMQS = 6;
    private static final int REPORT_THREAD_AMOUNT_LEAKTOMQS = 8;
    private static final int RESUME_LEAK = 5;
    private static final int START_FD_TRACK = 2;
    private static final int START_THREAD_TRACK = 3;
    private static final int START_TRACK = 1;
    private static final int START_TRACK_SIGNAL = 50;
    private static final String SYSPROP_ENABLE_RESUME_STRATEGY = "persist.sentinel.resume.enable";
    private static final String SYSPROP_ENABLE_TRACK_MALLOC = "persist.track.malloc.enable";
    private static final String SYSTEM_SERVER = "system_server";
    private static final int SYSTEM_SERVER_MAX_JAVAHEAP = 409600;
    private static final int SYSTEM_SERVER_MAX_NATIVEHEAP = 358400;
    private static final String TAG = "MiuiSentinelMemoryManager";
    private static final int TOTAL_RSS_LIMIT = 3145728;
    private static MiuiSentinelMemoryManager miuiSentinelMemoryManager;
    private Context mContext;
    private HandlerThread mMiuiSentineThread;
    private volatile MiuiSentineHandler mSentineHandler;
    private ActivityManagerService mService;
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.sys.mss", false);
    private static final String SYSPROP_ENABLE_SENTINEL_MEMORY_MONITOR = "persist.sys.debug.enable_sentinel_memory_monitor";
    public static final boolean ENABLE_SENTINEL_MEMORY_MONITOR = SystemProperties.getBoolean(SYSPROP_ENABLE_SENTINEL_MEMORY_MONITOR, false);
    private static final String SYSPROP_ENABLE_MQS_REPORT = "persist.sys.debug.enable_mqs_report";
    public static final boolean ENABLE_MQS_REPORT = SystemProperties.getBoolean(SYSPROP_ENABLE_MQS_REPORT, false);
    private static final HashSet<String> DIALOG_APP_LIST = new HashSet<String>() { // from class: com.miui.server.sentinel.MiuiSentinelMemoryManager.1
        {
            add("com.miui.miwallpaper");
            add(AccessController.PACKAGE_SYSTEMUI);
            add("com.example.memleaktesttool");
        }
    };
    private ConcurrentHashMap<String, Integer> eventList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NativeHeapUsageInfo> trackList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> trackEventList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> highCapacityRssList = new ConcurrentHashMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public enum Action {
        START_TRACK,
        REPORT_TRACK
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MiuiSentineHandler extends Handler {
        public MiuiSentineHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (MiuiSentinelMemoryManager.DEBUG) {
                Slog.d(MiuiSentinelMemoryManager.TAG, "information has been received and message.what = " + msg.what + "msg.when = " + msg.getWhen() + "System.currentTimeMillis = " + System.currentTimeMillis());
            }
            switch (msg.what) {
                case 1:
                    if (!MiuiSentinelMemoryManager.this.isProcessTracked((NativeHeapUsageInfo) msg.obj)) {
                        MiuiSentinelMemoryManager.this.enableNativeHeapTrack((NativeHeapUsageInfo) msg.obj, true);
                        return;
                    }
                    return;
                case 2:
                case 3:
                default:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "Unknown message");
                    return;
                case 4:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "end stack track, ready report event");
                    MiuiSentinelMemoryManager.this.enableNativeHeapTrack((NativeHeapUsageInfo) msg.obj, false);
                    return;
                case 5:
                    MiuiSentinelMemoryManager.this.resumeMemLeak(msg.obj);
                    return;
                case 6:
                    MiuiSentinelMemoryManager.this.reportNativeHeapLeak((NativeHeapUsageInfo) msg.obj);
                    return;
                case 7:
                    MiuiSentinelMemoryManager.this.reportJavaHeapLeak((JavaHeapUsageInfo) msg.obj);
                    return;
                case 8:
                    MiuiSentinelMemoryManager.this.reportThreadAmountLeak((ThreadUsageInfo) msg.obj);
                    return;
                case 9:
                    MiuiSentinelMemoryManager.this.reportFdAmountLeak((FdUsageInfo) msg.obj);
                    return;
            }
        }
    }

    public static MiuiSentinelMemoryManager getInstance() {
        if (miuiSentinelMemoryManager == null) {
            miuiSentinelMemoryManager = new MiuiSentinelMemoryManager();
        }
        return miuiSentinelMemoryManager;
    }

    public void init(ActivityManagerService mService, Context mContext) {
        this.mService = mService;
        this.mContext = mContext;
        ScoutMemoryError.getInstance().init(mService, mContext);
    }

    private MiuiSentinelMemoryManager() {
        HandlerThread handlerThread = new HandlerThread(HANDLER_NAME);
        this.mMiuiSentineThread = handlerThread;
        handlerThread.start();
        this.mSentineHandler = new MiuiSentineHandler(this.mMiuiSentineThread.getLooper());
        Slog.d(TAG, "MiuiSentinelMemoryManager init");
    }

    public boolean filterMessages(SocketPacket socketPacket) {
        String procname = socketPacket.getProcess_name();
        String type = socketPacket.getEvent_type();
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("#").append(procname);
        if (DEBUG) {
            Slog.e(TAG, "filtermessages item:" + sb.toString());
        }
        if (this.eventList.get(sb.toString()) == null) {
            this.eventList.put(sb.toString(), 1);
            return false;
        }
        if (this.eventList.get(sb.toString()).intValue() >= 3) {
            return true;
        }
        this.eventList.put(sb.toString(), Integer.valueOf(this.eventList.get(sb.toString()).intValue() + 1));
        return false;
    }

    public void judgmentRssLeakException(SocketPacket socketPacket) {
        RssUsageInfo rssUsageInfo = getRssinfo(socketPacket);
        long rssSize = rssUsageInfo.getRssSize();
        double percentage = (rssSize * 1.0d) / 3145728.0d;
        if (percentage > 0.2d) {
            StringBuilder sb = new StringBuilder();
            sb.append("RSS Leak in pid (" + rssUsageInfo.getPid() + ")" + rssUsageInfo.getName());
            sb.append("RSS size:" + rssUsageInfo.getRssSize());
            removeEventList(socketPacket);
            reportRssLeakEvent(416, sb.toString(), rssUsageInfo);
        }
    }

    public void judgmentNativeHeapLeakException(SocketPacket socketPacket) {
        NativeHeapUsageInfo nativeHeapUsageInfo = getNativeHeapinfo(socketPacket);
        if (SYSTEM_SERVER.equals(nativeHeapUsageInfo.getName())) {
            if (nativeHeapUsageInfo.getNativeHeapSize() > 358400 && this.highCapacityRssList.getOrDefault(SYSTEM_SERVER, 0).intValue() == nativeHeapUsageInfo.getPid()) {
                StringBuilder sb = new StringBuilder();
                sb.append(nativeHeapUsageInfo.getName()).append("#").append(nativeHeapUsageInfo.getPid());
                this.trackList.put(sb.toString(), nativeHeapUsageInfo);
                sendMessage(nativeHeapUsageInfo, 1);
                return;
            }
            return;
        }
        if (MiuiSentinelService.getAppNativeheapWhiteList().get(nativeHeapUsageInfo.getName()) != null) {
            long limit = MiuiSentinelService.getAppNativeheapWhiteList().get(nativeHeapUsageInfo.getName()).intValue();
            Slog.e(TAG, "app limit: " + limit);
            if (socketPacket.getGrowsize() > limit && this.highCapacityRssList.getOrDefault(nativeHeapUsageInfo.getName(), 0).intValue() == nativeHeapUsageInfo.getPid()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(nativeHeapUsageInfo.getName()).append("#").append(nativeHeapUsageInfo.getPid());
                this.trackList.put(sb2.toString(), nativeHeapUsageInfo);
                sendMessage(nativeHeapUsageInfo, 1);
            }
        }
    }

    public void judgmentJavaHeapLeakException(SocketPacket socketPacket) {
        if (!isEnableMqsReport()) {
            return;
        }
        JavaHeapUsageInfo javaHeapUsageInfo = getJavaHeapinfo(socketPacket);
        if (SYSTEM_SERVER.equals(javaHeapUsageInfo.getName())) {
            if (socketPacket.getGrowsize() > 409600) {
                sendMessage(javaHeapUsageInfo, 7);
            }
        } else {
            long limit = MiuiSentinelService.getAppJavaheapWhiteList().get(javaHeapUsageInfo.getName()).intValue();
            if (socketPacket.getGrowsize() > limit) {
                sendMessage(javaHeapUsageInfo, 7);
            }
        }
    }

    public void judgmentThreadAmountLeakException(SocketPacket socketPacket) {
        ThreadUsageInfo threadUsageInfo = getThreadUsageInfo(socketPacket);
        if (threadUsageInfo.getThreadAmount() > 500) {
            sendMessage(threadUsageInfo, 8);
        }
    }

    public void judgmentFdAmountLeakException(SocketPacket socketPacket) {
        FdUsageInfo fdUsageInfo = getFdUsageInfo(socketPacket);
        if (fdUsageInfo.getFd_amount() > 1000) {
            sendMessage(fdUsageInfo, 9);
        }
    }

    private void reportRssLeakEvent(int type, String subject, RssUsageInfo rssUsageInfo) {
        int pid = rssUsageInfo.getPid();
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        if (packageName == null) {
            packageName = MiuiSentinelUtils.getProcessCmdline(pid);
        }
        if ("unknown".equals(packageName)) {
            Slog.d(TAG, "The current process may exit and ignore report mqs");
            return;
        }
        File filename = MiuiSentinelUtils.getExceptionPath(pid, packageName, "RSS_LEAK");
        MiuiSentinelUtils.dumpRssInfo(pid, rssUsageInfo.getRssSize(), packageName, filename);
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        event.setType(type);
        event.setPid(pid);
        event.setPackageName(packageName);
        event.setProcessName(packageName);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(subject);
        event.setDetails(subject);
        reportMemLeakMisight(event);
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
    }

    private void reportHeapLeakEvent(int type, String subject, String stacktrace, int pid, String procname, long growsize) {
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        List<String> extraFile = new ArrayList<>();
        extraFile.add(MEMLEAK_DIR);
        event.setExtraFiles(extraFile);
        event.setType(type);
        event.setPid(pid);
        event.setPackageName(procname);
        event.setProcessName(procname);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(subject);
        event.setDetails(stacktrace);
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
        reportMemLeakMisight(event);
    }

    private void reportMemLeakMisight(ExceptionEvent event) {
        MiEvent miEvent;
        if (event == null) {
            return;
        }
        switch (event.getType()) {
            case 416:
                miEvent = new MiEvent(901004203);
                miEvent.addStr("RssInfo", getDfxDetails(event.getDetails()));
                break;
            case 417:
                miEvent = new MiEvent(901004204);
                miEvent.addStr("Stacktrace", getDfxDetails(event.getDetails()));
                break;
            case 418:
                miEvent = new MiEvent(901004205);
                miEvent.addStr("Stacktrace", getDfxDetails(event.getDetails()));
                break;
            default:
                return;
        }
        miEvent.addStr("Summary", event.getSummary()).addStr("PackageName", event.getPackageName()).addLong("CurrentTime", System.currentTimeMillis());
        MiSight.sendEvent(miEvent);
    }

    private String getDfxDetails(String details) {
        return details.length() > 2000 ? details.substring(0, 2000) : details;
    }

    private void reportLeakEvent(int type, String subject, String stacktrace, int pid, String procname) {
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        event.setType(type);
        event.setPid(pid);
        event.setPackageName(procname);
        event.setProcessName(procname);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(subject);
        event.setDetails(stacktrace);
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
    }

    public RssUsageInfo getRssinfo(SocketPacket socketPacket) {
        RssUsageInfo info = new RssUsageInfo();
        Slog.e(TAG, "getRSSinfo: " + socketPacket.getProcess_name());
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setRssSize(socketPacket.getGrowsize());
        socketPacket.getData();
        Slog.d(TAG, "cmdline: " + info.getName() + "pid: " + info.getPid() + "RSSsize:" + info.getRssSize());
        return info;
    }

    public NativeHeapUsageInfo getNativeHeapinfo(SocketPacket socketPacket) {
        NativeHeapUsageInfo info = new NativeHeapUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setNativeHeapSize(socketPacket.getGrowsize());
        return info;
    }

    public JavaHeapUsageInfo getJavaHeapinfo(SocketPacket socketPacket) {
        JavaHeapUsageInfo info = new JavaHeapUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setJavaHeapSize(socketPacket.getGrowsize());
        return info;
    }

    public ThreadUsageInfo getThreadUsageInfo(SocketPacket socketPacket) {
        ThreadUsageInfo info = new ThreadUsageInfo();
        Slog.e(TAG, "getThreadinfo: " + socketPacket.getProcess_name());
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setThreadAmount(socketPacket.getGrowsize());
        info.setUsageInfo(socketPacket.getData());
        return info;
    }

    public FdUsageInfo getFdUsageInfo(SocketPacket socketPacket) {
        FdUsageInfo info = new FdUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setFd_amount(socketPacket.getGrowsize());
        info.setUsageInfo(socketPacket.getData());
        return info;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resumeMemLeak(Object object) {
        if (!SystemProperties.getBoolean(SYSPROP_ENABLE_RESUME_STRATEGY, false)) {
            Slog.e(TAG, "no enable resume tactics ！");
            return;
        }
        if (object instanceof RssUsageInfo) {
            RssUsageInfo rssUsageInfo = (RssUsageInfo) object;
            int adj = ProcessUtils.getCurAdjByPid(rssUsageInfo.getPid());
            resumeMemLeak(rssUsageInfo.getPid(), adj, rssUsageInfo.getRssSize(), rssUsageInfo.getName(), "RSS is too large, leak occurred", rssUsageInfo.getMaxIncrease());
        } else if (object instanceof NativeHeapUsageInfo) {
            NativeHeapUsageInfo nativeHeapUsageInfo = (NativeHeapUsageInfo) object;
            int adj2 = ProcessUtils.getCurAdjByPid(nativeHeapUsageInfo.getPid());
            resumeMemLeak(nativeHeapUsageInfo.getPid(), adj2, nativeHeapUsageInfo.getNativeHeapSize(), nativeHeapUsageInfo.getName(), "Native Heap is too large, leak occurred", "NativeHeap");
        } else if (object instanceof JavaHeapUsageInfo) {
            JavaHeapUsageInfo javaHeapUsageInfo = (JavaHeapUsageInfo) object;
            int adj3 = ProcessUtils.getCurAdjByPid(javaHeapUsageInfo.getPid());
            resumeMemLeak(javaHeapUsageInfo.getPid(), adj3, javaHeapUsageInfo.getJavaHeapSize(), javaHeapUsageInfo.getName(), "Java Heap is too large, leak occurred", "JavaHeap");
        }
    }

    private void resumeMemLeak(int pid, int adj, long size, String name, String reason, String type) {
        if (!MiuiSentinelUtils.isLaboratoryTest()) {
            if (DIALOG_APP_LIST.contains(name)) {
                ProcessRecord app = ProcessUtils.getProcessRecordByPid(pid);
                ScoutMemoryError.getInstance().showAppMemoryErrorDialog(app, name + "(" + pid + ") used" + size + "kB" + reason);
                return;
            }
            return;
        }
        if (pid > 0 && Process.getThreadGroupLeader(pid) == pid) {
            if (adj == -900 && pid == Process.myPid()) {
                Slog.e(TAG, "system_server(" + pid + ") use " + type + size + "kb too many occurring" + reason);
                throw new RuntimeException("system_server (" + pid + ") used " + size + "kB " + reason);
            }
            if (this.mService == null || adj <= -900 || adj >= 1001) {
                Slog.e(TAG, "Kill " + name + "(" + pid + "), Used " + size + "kB " + reason);
                Process.killProcess(pid);
                return;
            }
            ProcessRecord app2 = ProcessUtils.getProcessRecordByPid(pid);
            String appReason = name + "(" + pid + ") used" + size + "kB" + reason;
            boolean killAction = false;
            if (ScoutMemoryError.getInstance().scheduleCrashApp(app2, appReason)) {
                killAction = true;
            }
            if (!killAction) {
                appReason = "Kill" + name + "(" + pid + ") used" + size + "kB" + reason;
                Process.killProcess(pid);
            }
            Slog.e(TAG, appReason);
            return;
        }
        Slog.e(TAG, name + "(" + pid + ") is invalid");
    }

    public <T> void sendMessage(T obj, int number) {
        Message message = miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
        message.what = number;
        message.obj = obj;
        miuiSentinelMemoryManager.mSentineHandler.sendMessage(message);
        if (DEBUG) {
            Slog.d(TAG, "msg.what = " + message.what + "msg.obj = " + message.obj.toString());
        }
    }

    public <T> void sendMessage(T obj, int number, long timeout) {
        Message message = miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
        message.what = number;
        message.obj = obj;
        miuiSentinelMemoryManager.mSentineHandler.sendMessageDelayed(message, timeout);
        if (DEBUG) {
            Slog.d(TAG, "msg.what = " + message.what + "send message time = " + System.currentTimeMillis());
        }
    }

    public boolean isEnableSentinelMemoryMonitor() {
        if (ENABLE_SENTINEL_MEMORY_MONITOR) {
            return true;
        }
        return false;
    }

    public boolean isEnableMqsReport() {
        if (ENABLE_MQS_REPORT) {
            return true;
        }
        return false;
    }

    public void removeEventList(SocketPacket socketPacket) {
        StringBuilder sb = new StringBuilder();
        String procname = socketPacket.getProcess_name();
        String type = socketPacket.getEvent_type();
        sb.append(type).append("#").append(procname);
        this.eventList.remove(sb.toString());
    }

    public boolean handlerTriggerTrack(int pid, Action action) {
        if (pid <= 0) {
            Slog.e(TAG, "Failed to enable Track! pid is invalid");
            return false;
        }
        if (MiuiSentinelUtils.isEnaleTrack()) {
            String mapsPath = "/proc/" + String.valueOf(pid) + "/maps";
            if (isLibraryExist(mapsPath)) {
                switch (AnonymousClass2.$SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action[action.ordinal()]) {
                    case 1:
                        Slog.e(TAG, "begin pid(" + pid + ") Track");
                        Process.sendSignal(pid, 50);
                        break;
                    case 2:
                        Slog.e(TAG, "report pid(" + pid + ") Track");
                        Process.sendSignal(pid, END_TRACK_SIGNAL);
                        break;
                }
                Slog.e(TAG, "enable track malloc sucess!");
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.miui.server.sentinel.MiuiSentinelMemoryManager$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action;

        static {
            int[] iArr = new int[Action.values().length];
            $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action = iArr;
            try {
                iArr[Action.START_TRACK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action[Action.REPORT_TRACK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private boolean isLibraryExist(String path) {
        BufferedReader reader;
        String mapsInfo;
        StringBuilder libraryName = new StringBuilder();
        libraryName.append("lib" + SystemProperties.get(SYSPROP_ENABLE_TRACK_MALLOC) + ".so");
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        do {
            try {
                mapsInfo = reader.readLine();
                if (mapsInfo == null) {
                    reader.close();
                    Slog.e(TAG, path + " not found track's library: " + libraryName.toString());
                    return false;
                }
            } finally {
            }
        } while (!mapsInfo.contains(libraryName));
        Slog.e(TAG, path + " found track's library");
        reader.close();
        return true;
    }

    public void outPutTrackLog(TrackPacket trackPacket) throws IOException {
        File trackDir = new File(MEMLEAK_DIR);
        if (!trackDir.exists()) {
            if (!trackDir.mkdirs()) {
                Slog.e(TAG, "cannot create memleak Dir", new Throwable());
            }
            FileUtils.setPermissions(trackDir, 508, -1, -1);
        }
        String dirSuffix = MiuiSentinelUtils.getFormatDateTime(System.currentTimeMillis());
        String dirname = trackPacket.getPid() + d.h + trackPacket.getProcess_name() + d.h + dirSuffix;
        File exceptionDir = new File(trackDir, dirname);
        if (!exceptionDir.exists()) {
            if (!exceptionDir.mkdirs()) {
                Slog.e(TAG, "cannot create exceptionDir", new Throwable());
            }
            FileUtils.setPermissions(exceptionDir, 508, -1, -1);
        }
        String filename = trackPacket.getPid() + d.h + trackPacket.getProcess_name() + d.h + dirSuffix + "_heapleak_info.txt";
        File trackfile = new File(exceptionDir, filename);
        if (!trackfile.exists()) {
            if (!trackfile.createNewFile()) {
                Slog.e(TAG, "cannot create leakfile", new Throwable());
            }
            FileUtils.setPermissions(trackfile.getAbsolutePath(), 508, -1, -1);
        }
        try {
            FileWriter writers = new FileWriter(trackfile, true);
            try {
                writers.write(trackPacket.getData());
                writers.close();
            } finally {
            }
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write Track Stack to file", new Throwable());
        }
    }

    public void reportNativeHeapLeak(NativeHeapUsageInfo nativeHeapUsageInfo) {
        String nativeleakmsg = "NativeHeap Leak Proc info Name: " + nativeHeapUsageInfo.getName() + " Pid = " + nativeHeapUsageInfo.getPid() + " NativeHeap Size = " + nativeHeapUsageInfo.getNativeHeapSize() + " KB (Threshold = " + MiuiSentinelUtils.getProcessThreshold(nativeHeapUsageInfo.getName(), 18) + "KB) ";
        Slog.d(TAG, "debug mqs info:" + nativeHeapUsageInfo.toString());
        reportHeapLeakEvent(417, nativeleakmsg, nativeHeapUsageInfo.getStackTrace(), nativeHeapUsageInfo.getPid(), nativeHeapUsageInfo.getName(), nativeHeapUsageInfo.getNativeHeapSize());
        sendMessage(nativeHeapUsageInfo, 5, 0L);
    }

    public void reportJavaHeapLeak(JavaHeapUsageInfo javaHeapUsageInfo) {
        String javaleakmsg = "JavaHeap Leak " + javaHeapUsageInfo.toString() + " (Threshold = " + MiuiSentinelUtils.getProcessThreshold(javaHeapUsageInfo.getName(), 17) + "KB)";
        reportHeapLeakEvent(418, javaleakmsg, "", javaHeapUsageInfo.getPid(), javaHeapUsageInfo.getName(), javaHeapUsageInfo.getJavaHeapSize());
        sendMessage(javaHeapUsageInfo, 5, 0L);
    }

    public void reportThreadAmountLeak(ThreadUsageInfo threadUsageInfo) {
        String packageName;
        String thleakmsg = "Thread Amount Leak " + threadUsageInfo.toString() + " (Threshold = 500 )";
        int pid = threadUsageInfo.getPid();
        String packageName2 = ProcessUtils.getPackageNameByPid(pid);
        if (packageName2 != null) {
            packageName = packageName2;
        } else {
            packageName = MiuiSentinelUtils.getProcessCmdline(pid);
        }
        if ("unknown".equals(packageName)) {
            Slog.d(TAG, "The current process may exit and ignore report mqs");
            return;
        }
        File filename = MiuiSentinelUtils.getExceptionPath(pid, packageName, "Thread_LEAK");
        MiuiSentinelUtils.dumpThreadInfo(pid, packageName, filename);
        reportLeakEvent(440, thleakmsg, threadUsageInfo.getUsageInfo(), pid, packageName);
    }

    public void reportFdAmountLeak(FdUsageInfo fdUsageInfo) {
        String fdleakmsg = "Fd Amount Leak " + fdUsageInfo.toString() + " (Threshold = 1000 )";
        reportLeakEvent(432, fdleakmsg, fdUsageInfo.getUsageInfo(), fdUsageInfo.getPid(), fdUsageInfo.getName());
    }

    public void enableNativeHeapTrack(NativeHeapUsageInfo nativeHeapUsageInfo, boolean isEnable) {
        if (isEnable) {
            handlerTriggerTrack(nativeHeapUsageInfo.getPid(), Action.START_TRACK);
            Slog.d(TAG, "begin stack track ");
            sendMessage(nativeHeapUsageInfo, 4, 300000L);
        } else {
            removeTrackEvent(nativeHeapUsageInfo);
            handlerTriggerTrack(nativeHeapUsageInfo.getPid(), Action.REPORT_TRACK);
        }
    }

    public boolean isProcessTracked(NativeHeapUsageInfo nativeHeapUsageInfo) {
        String key = nativeHeapUsageInfo.getName() + "#" + nativeHeapUsageInfo.getPid();
        if (this.trackEventList.get(key) != null) {
            return true;
        }
        this.trackEventList.put(key, 1);
        return false;
    }

    public void removeTrackEvent(NativeHeapUsageInfo nativeHeapUsageInfo) {
        String key = nativeHeapUsageInfo.getName() + "#" + nativeHeapUsageInfo.getPid();
        this.trackEventList.remove(key);
    }

    public ConcurrentHashMap<String, NativeHeapUsageInfo> getTrackList() {
        return this.trackList;
    }
}
