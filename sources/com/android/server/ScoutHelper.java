package com.android.server;

import android.os.FileUtils;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.perfdebug.MessageMonitor;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.xiaomi.abtest.d.d;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import miui.mqsas.IMQSNative;
import miui.mqsas.scout.ScoutUtils;
import miui.os.Build;

/* loaded from: classes.dex */
public class ScoutHelper {
    public static final String ACTION_CAT = "cat";
    public static final String ACTION_CP = "cp";
    public static final String ACTION_DMABUF_DUMP = "dmabuf_dump";
    public static final String ACTION_DUMPSYS = "dumpsys";
    public static final String ACTION_LOGCAT = "logcat";
    public static final String ACTION_MV = "mv";
    public static final String ACTION_PS = "ps";
    public static final String ACTION_TOP = "top";
    public static final String BINDER_ASYNC = "    pending async transaction";
    public static final String BINDER_ASYNC_GKI = "MIUI    pending async transaction";
    public static final String BINDER_ASYNC_MIUI = "pending async transaction";
    public static final String BINDER_CONTEXT = "context";
    public static final String BINDER_DUR = "duration:";
    public static final String BINDER_FS_PATH_GKI = "/dev/binderfs/binder_logs/proc/";
    public static final String BINDER_FS_PATH_MIUI = "/dev/binderfs/binder_logs/proc_transaction/";
    public static final int BINDER_FULL_KILL_SCORE_ADJ_MIN = -800;
    public static final String BINDER_INCOMING = "    incoming transaction";
    public static final String BINDER_INCOMING_GKI = "MIUI    incoming transaction";
    public static final String BINDER_INCOMING_MIUI = "incoming transaction";
    public static final String BINDER_OUTGOING = "    outgoing transaction";
    public static final String BINDER_OUTGOING_GKI = "MIUI    outgoing transaction";
    public static final String BINDER_OUTGOING_MIUI = "outgoing transaction";
    public static final String BINDER_PENDING = "    pending transaction";
    public static final String BINDER_PENDING_MIUI = "pending transaction";
    public static final int BINDER_WAITTIME_THRESHOLD = 2;
    public static final int CALL_TYPE_APP = 1;
    public static final int CALL_TYPE_SYSTEM = 0;
    private static String CONSOLE_RAMOOPS_0_PATH = null;
    private static String CONSOLE_RAMOOPS_PATH = null;
    private static final boolean DEBUG = false;
    public static final int DEFAULT_RUN_COMMAND_TIMEOUT = 60;
    public static final boolean DISABLE_AOSP_ANR_TRACE_POLICY;
    public static final String FILE_DIR_MQSAS = "/data/mqsas/";
    public static final String FILE_DIR_SCOUT = "scout";
    public static final String FILE_DIR_STABILITY = "/data/miuilog/stability";
    public static final boolean IS_INTERNATIONAL_BUILD;
    public static final int JAVA_PROCESS = 1;
    private static final List<String> JAVA_SHELL_PROCESS;
    public static final int MONKEY_BLOCK_THRESHOLD = 20;
    private static final String MONKEY_PROCESS = "com.android.commands.monkey";
    private static final String MQSASD = "miui.mqsas.IMQSNative";
    public static final String MQS_PSTORE_DIR = "/data/mqsas/temp/pstore/";
    public static final int NATIVE_PROCESS = 2;
    public static final int OOM_SCORE_ADJ_MAX = 1000;
    public static final int OOM_SCORE_ADJ_MIN = -1000;
    public static final int OOM_SCORE_ADJ_SHELL = -950;
    public static final boolean PANIC_ANR_D_THREAD;
    private static String PROC_VERSION = null;
    public static final int PS_MODE_FULL = 1;
    public static final int PS_MODE_NORMAL = 0;
    private static final String REGEX_PATTERN = "\\bfrom((?:\\s+)?\\d+):((?:\\s+)?\\d+)\\s+to((?:\\s+)?\\d+):((?:\\s+)?\\d+)+.*code:((?:\\s+)?\\d+).*duration:((?:\\s+)?(\\d+(?:\\.\\d+)?))\\b";
    private static final String REGEX_PATTERN_LINUX_VERSION = "\\bLinux version\\s+(\\d+\\.\\d+).";
    private static final String REGEX_PATTERN_NEW = "\\bfrom((?:\\s+)?\\d+):((?:\\s+)?\\d+)\\s+to((?:\\s+)?\\d+):((?:\\s+)?\\d+)+.*code((?:\\s+)?\\d+)+.*elapsed((?:\\s+)?\\d+)ms+.*";
    public static final boolean SCOUT_BINDER_GKI;
    public static final int SHELL_PROCESS = 3;
    private static final String[] STATUS_KEYS;
    public static final boolean SYSRQ_ANR_D_THREAD;
    public static final int SYSTEM_ADJ = -900;
    private static final String TAG = "ScoutHelper";
    public static final int UNKNOW_PROCESS = 0;
    public static double linuxVersion;
    private static IMQSNative mDaemon;
    private static final String[] sSkipProcs;
    private static Boolean supportNewBinderLog;
    public static final boolean ENABLED_SCOUT = SystemProperties.getBoolean("persist.sys.miui_scout_enable", false);
    public static final boolean ENABLED_SCOUT_DEBUG = SystemProperties.getBoolean("persist.sys.miui_scout_debug", false);
    public static final boolean BINDER_FULL_KILL_PROC = SystemProperties.getBoolean("persist.sys.miui_scout_binder_full_kill_process", false);
    public static final boolean PANIC_D_THREAD = SystemProperties.getBoolean("persist.sys.panicOnWatchdog_D_state", false);

    /* loaded from: classes.dex */
    public static class ProcStatus {
        public int tracerPid = -1;
    }

    static {
        SYSRQ_ANR_D_THREAD = SystemProperties.getBoolean("persist.sys.sysrqOnAnr_D_state", false) || SystemProperties.getBoolean("persist.sys.panicOnAnr_D_state", false);
        PANIC_ANR_D_THREAD = SystemProperties.getBoolean("persist.sys.panicOnAnr_D_state", false);
        SCOUT_BINDER_GKI = SystemProperties.getBoolean("persist.sys.scout_binder_gki", false);
        DISABLE_AOSP_ANR_TRACE_POLICY = SystemProperties.getBoolean("persist.sys.disable_aosp_anr_policy", false);
        IS_INTERNATIONAL_BUILD = Build.IS_INTERNATIONAL_BUILD;
        CONSOLE_RAMOOPS_PATH = "/sys/fs/pstore/console-ramoops";
        CONSOLE_RAMOOPS_0_PATH = "/sys/fs/pstore/console-ramoops-0";
        PROC_VERSION = "proc/version";
        linuxVersion = 0.0d;
        supportNewBinderLog = false;
        try {
            BufferedReader versionReader = new BufferedReader(new FileReader(PROC_VERSION));
            try {
                String cmdLine = versionReader.readLine().trim();
                Pattern pattern = Pattern.compile(REGEX_PATTERN_LINUX_VERSION);
                Matcher matcher = pattern.matcher(cmdLine);
                if (!matcher.find()) {
                    Slog.w(TAG, "Parsing Linux version failed. info : " + cmdLine);
                } else {
                    double doubleValue = Double.valueOf(matcher.group(1)).doubleValue();
                    linuxVersion = doubleValue;
                    if (doubleValue >= 6.1d) {
                        supportNewBinderLog = true;
                    }
                    Slog.d(TAG, "The current Linux version is " + linuxVersion + " supportNewBinderLog = " + supportNewBinderLog);
                }
                versionReader.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.w(TAG, "Read PROC_VERSION Error " + e.toString());
        }
        JAVA_SHELL_PROCESS = Arrays.asList(MONKEY_PROCESS);
        sSkipProcs = new String[]{"com.android.networkstack.process"};
        STATUS_KEYS = new String[]{"TracerPid:"};
    }

    private static IMQSNative getmDaemon() {
        if (mDaemon == null) {
            IMQSNative asInterface = IMQSNative.Stub.asInterface(ServiceManager.getService(MQSASD));
            mDaemon = asInterface;
            if (asInterface == null) {
                Slog.e(TAG, "mqsasd not available!");
            }
        }
        return mDaemon;
    }

    /* loaded from: classes.dex */
    public static class Action {
        private List<String> actions = new ArrayList();
        private List<String> params = new ArrayList();
        private List<String> includeFiles = new ArrayList();

        public String toString() {
            return "Action{actions=" + this.actions + ", params=" + this.params + ", includeFiles=" + this.includeFiles + '}';
        }

        public void addActionAndParam(String action, String param) {
            this.actions.add(action);
            if (param == null) {
                param = "";
            }
            this.params.add(param);
        }

        public void addIncludeFile(String path) {
            if (TextUtils.isEmpty(path)) {
                return;
            }
            this.includeFiles.add(path);
        }

        public void addIncludeFiles(List<String> includeFiles) {
            if (includeFiles != null && includeFiles.size() > 0) {
                for (String path : includeFiles) {
                    addIncludeFile(path);
                }
            }
        }

        public void clearIncludeFiles() {
            List<String> list = this.includeFiles;
            if (list != null) {
                list.clear();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ScoutBinderInfo {
        private StringBuilder mBinderTransInfo;
        private int mCallType;
        private int mFromPid;
        private boolean mHasDThread;
        private int mMonkeyPid;
        private int mPid;
        private StringBuilder mProcInfo;
        private String mTag;

        public ScoutBinderInfo(int mPid, int mCallType, String mTag) {
            this(mPid, 0, mCallType, mTag);
        }

        public ScoutBinderInfo(int mPid, int mFromPid, int mCallType, String mTag) {
            this.mHasDThread = false;
            this.mBinderTransInfo = new StringBuilder();
            this.mPid = mPid;
            this.mFromPid = mFromPid;
            this.mTag = mTag;
            this.mCallType = mCallType;
            this.mProcInfo = new StringBuilder();
        }

        public void setDThreadState(boolean mHasDThread) {
            this.mHasDThread = mHasDThread;
        }

        public void setMonkeyPid(int monkeyPid) {
            this.mMonkeyPid = monkeyPid;
        }

        public boolean getDThreadState() {
            return this.mHasDThread;
        }

        public int getMonkeyPid() {
            return this.mMonkeyPid;
        }

        public void addBinderTransInfo(String sInfo) {
            this.mBinderTransInfo.append(sInfo + "\n");
        }

        public String getBinderTransInfo() {
            String info = this.mBinderTransInfo.toString();
            if (info == "") {
                return "Here are no Binder-related exception messages available.";
            }
            return "Binder Tracsaction Info:\n" + info;
        }

        public int getPid() {
            return this.mPid;
        }

        public int getFromPid() {
            return this.mFromPid;
        }

        public int getCallType() {
            return this.mCallType;
        }

        public String getTag() {
            return this.mTag;
        }

        public void setProcInfo(String procInfo) {
            this.mProcInfo.append(procInfo + "\n");
        }

        public String getProcInfo() {
            String info = this.mProcInfo.toString();
            return info;
        }
    }

    public static Boolean CheckDState(String tag, int pid) {
        return CheckDState(tag, pid, null);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(21:10|11|12|13|14|15|16|17|(4:(3:84|85|(4:91|63|64|65))|63|64|65)|19|(1:25)|49|50|51|52|53|54|55|(3:56|57|(1:59)(1:60))|61|62) */
    /* JADX WARN: Can't wrap try/catch for region: R(24:(4:(4:118|119|120|121)|119|120|121)|133|134|135|136|137|138|139|140|141|142|143|144|145|146|147|148|149|150|151|152|(3:153|154|(1:156)(1:157))|158|159) */
    /* JADX WARN: Can't wrap try/catch for region: R(32:106|107|108|109|(4:(4:118|119|120|121)|119|120|121)|124|(2:126|(2:128|(1:130)(1:200))(1:201))(1:202)|131|132|133|134|135|136|137|138|139|140|141|142|143|144|145|146|147|148|149|150|151|152|(3:153|154|(1:156)(1:157))|158|159) */
    /* JADX WARN: Can't wrap try/catch for region: R(35:106|107|108|109|(4:118|119|120|121)|124|(2:126|(2:128|(1:130)(1:200))(1:201))(1:202)|131|132|133|134|135|136|137|138|139|140|141|142|143|144|145|146|147|148|149|150|151|152|(3:153|154|(1:156)(1:157))|158|159|119|120|121) */
    /* JADX WARN: Code restructure failed: missing block: B:176:0x0202, code lost:
    
        r11 = r22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:179:0x01fa, code lost:
    
        r34 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:182:0x01fe, code lost:
    
        r34 = r4;
        r30 = r11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:185:0x0206, code lost:
    
        r34 = r4;
        r18 = r10;
        r30 = r11;
        r11 = r22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x0392, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x0393, code lost:
    
        android.util.Slog.w(r1, "Failed to read " + r7 + " :" + r0.toString());
     */
    /* JADX WARN: Removed duplicated region for block: B:39:0x040f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.Boolean CheckDState(java.lang.String r35, int r36, com.android.server.ScoutHelper.ScoutBinderInfo r37) {
        /*
            Method dump skipped, instructions count: 1057
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ScoutHelper.CheckDState(java.lang.String, int, com.android.server.ScoutHelper$ScoutBinderInfo):java.lang.Boolean");
    }

    private static Boolean supportGKI() {
        return Boolean.valueOf(supportNewBinderLog.booleanValue() || SCOUT_BINDER_GKI);
    }

    public static File getBinderLogFile(String tag, int pid) {
        File fileBinderMIUI = new File(BINDER_FS_PATH_MIUI + String.valueOf(pid));
        File fileBinderGKI = new File(BINDER_FS_PATH_GKI + String.valueOf(pid));
        if (fileBinderMIUI.exists()) {
            return fileBinderMIUI;
        }
        if (supportGKI().booleanValue() && fileBinderGKI.exists()) {
            return fileBinderGKI;
        }
        Slog.w(tag, "gki binder logfs or miui binder logfs are not exist");
        return null;
    }

    public static boolean addPidtoList(String tag, int pid, ArrayList<Integer> javaProcess, ArrayList<Integer> nativeProcess) {
        int adj = getOomAdjOfPid(tag, pid);
        int isJavaOrNativeProcess = checkIsJavaOrNativeProcess(adj);
        if (isJavaOrNativeProcess == 0) {
            return false;
        }
        if (isJavaOrNativeProcess == 1 && !javaProcess.contains(Integer.valueOf(pid))) {
            javaProcess.add(Integer.valueOf(pid));
            return true;
        }
        if (isJavaOrNativeProcess != 2 || nativeProcess.contains(Integer.valueOf(pid))) {
            return false;
        }
        nativeProcess.add(Integer.valueOf(pid));
        return true;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(13:11|(3:13|14|(5:16|(2:18|(1:20))(1:24)|21|22|23))|40|41|(2:43|44)|45|46|(6:48|49|51|21|22|23)(21:61|62|63|64|65|66|67|68|69|70|(3:107|108|109)(3:72|73|74)|75|76|77|79|80|82|(1:96)(6:85|86|(1:89)|90|91|(1:93))|94|95|23)|54|55|56|57|23) */
    /* JADX WARN: Code restructure failed: missing block: B:126:0x023a, code lost:
    
        r18 = r9;
        r23 = r10;
        r21 = r12;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static boolean checkAsyncBinderCallPidList(int r28, int r29, com.android.server.ScoutHelper.ScoutBinderInfo r30, java.util.ArrayList<java.lang.Integer> r31, java.util.ArrayList<java.lang.Integer> r32) {
        /*
            Method dump skipped, instructions count: 661
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ScoutHelper.checkAsyncBinderCallPidList(int, int, com.android.server.ScoutHelper$ScoutBinderInfo, java.util.ArrayList, java.util.ArrayList):boolean");
    }

    /* JADX WARN: Can't wrap try/catch for region: R(8:21|(3:187|188|(4:195|178|179|76))|23|24|(2:26|27)|28|29|(8:172|173|174|175|177|178|179|76)(26:31|32|33|34|35|(3:159|160|161)(3:37|38|39)|40|41|42|43|44|45|46|47|48|49|50|51|52|53|55|(1:112)(2:59|(4:108|109|110|76)(6:61|62|(2:77|(2:91|(5:97|(1:101)|102|(1:104)(1:106)|105))(4:(1:84)|85|(1:89)|90))(4:66|(1:71)|72|73)|74|75|76))|107|74|75|76)) */
    /* JADX WARN: Code restructure failed: missing block: B:186:0x03fa, code lost:
    
        r30 = r5;
        r18 = r8;
        r28 = r10;
        r25 = r11;
        r21 = r12;
        r23 = r13;
        r32 = r15;
     */
    /* JADX WARN: Unreachable blocks removed: 2, instructions: 8 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static boolean checkBinderCallPidList(int r38, com.android.server.ScoutHelper.ScoutBinderInfo r39, java.util.ArrayList<java.lang.Integer> r40, java.util.ArrayList<java.lang.Integer> r41) {
        /*
            Method dump skipped, instructions count: 1147
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ScoutHelper.checkBinderCallPidList(int, com.android.server.ScoutHelper$ScoutBinderInfo, java.util.ArrayList, java.util.ArrayList):boolean");
    }

    public static boolean checkIsMonkey(String processName, double time) {
        return MONKEY_PROCESS.equals(processName) && time > 20.0d && ScoutUtils.isLibraryTest();
    }

    /* JADX WARN: Can't wrap try/catch for region: R(13:11|12|(3:14|15|(5:17|(2:19|(1:21))(1:25)|22|23|24))|37|38|(2:40|41)|42|43|(7:45|46|48|(1:50)|22|23|24)(14:56|57|58|59|(2:61|62)(2:96|97)|63|64|65|66|67|(2:69|(2:82|83)(5:71|72|73|74|76))(2:84|85)|77|78|24)|53|54|55|24) */
    /* JADX WARN: Code restructure failed: missing block: B:100:0x0201, code lost:
    
        r17 = r5;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void checkBinderThreadFull(int r25, com.android.server.ScoutHelper.ScoutBinderInfo r26, java.util.TreeMap<java.lang.Integer, java.lang.Integer> r27, java.util.ArrayList<java.lang.Integer> r28, java.util.ArrayList<java.lang.Integer> r29) {
        /*
            Method dump skipped, instructions count: 607
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ScoutHelper.checkBinderThreadFull(int, com.android.server.ScoutHelper$ScoutBinderInfo, java.util.TreeMap, java.util.ArrayList, java.util.ArrayList):void");
    }

    public static String resumeBinderThreadFull(String tag, TreeMap<Integer, Integer> inPidMap) {
        if (ENABLED_SCOUT_DEBUG) {
            Slog.d(tag, "Debug: resumeBinderFull");
        }
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(inPidMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() { // from class: com.android.server.ScoutHelper.1
            @Override // java.util.Comparator
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().intValue() - o1.getValue().intValue();
            }
        });
        StringBuilder minfo = new StringBuilder();
        minfo.append("Incoming Binder Procsss Info:");
        boolean isKilled = false;
        int[] skipPids = Process.getPidsForCommands(sSkipProcs);
        for (Map.Entry<Integer, Integer> mapping : list) {
            int inPid = mapping.getKey().intValue();
            int count = mapping.getValue().intValue();
            int oomAdj = getOomAdjOfPid(tag, inPid);
            if (BINDER_FULL_KILL_PROC && oomAdj >= -800 && !isKilled && inPid > 0 && !ArrayUtils.contains(skipPids, inPid)) {
                Slog.w(tag, "Pid(" + inPid + ") adj(" + oomAdj + ") is Killed, Because it use " + count + " binder thread of System_server");
                Process.killProcess(inPid);
                isKilled = true;
            }
            minfo.append("\nPid " + mapping.getKey() + "(adj : " + oomAdj + ") count " + mapping.getValue());
        }
        Slog.d(tag, minfo.toString());
        return minfo.toString();
    }

    public static void printfProcBinderInfo(int Pid, String tag) {
        File fileBinderReader = getBinderLogFile(TAG, Pid);
        if (fileBinderReader == null) {
            return;
        }
        Slog.w(tag, "Pid " + Pid + " Binder Info:");
        try {
            BufferedReader BinderReader = new BufferedReader(new FileReader(fileBinderReader));
            while (true) {
                try {
                    String binderTransactionInfo = BinderReader.readLine();
                    if (binderTransactionInfo != null) {
                        if (supportNewBinderLog.booleanValue()) {
                            if (binderTransactionInfo.startsWith(BINDER_OUTGOING) || binderTransactionInfo.startsWith(BINDER_INCOMING) || binderTransactionInfo.startsWith(BINDER_ASYNC) || binderTransactionInfo.startsWith(BINDER_PENDING)) {
                                Slog.w(tag, binderTransactionInfo);
                            }
                        } else if (SCOUT_BINDER_GKI) {
                            if (binderTransactionInfo.startsWith("MIUI")) {
                                Slog.w(tag, binderTransactionInfo);
                            }
                        } else {
                            Slog.w(tag, binderTransactionInfo);
                        }
                    } else {
                        BinderReader.close();
                        return;
                    }
                } finally {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Slog.w(TAG, "Read binder Proc transaction Error:", e);
        }
    }

    public static int checkIsJavaOrNativeProcess(int adj) {
        if (adj == -1000) {
            return 2;
        }
        if (adj == -950) {
            return 3;
        }
        return (adj < -900 || adj > 1000) ? 0 : 1;
    }

    public static int getOomAdjOfPid(String tag, int Pid) {
        int adj = OOM_SCORE_ADJ_MIN;
        try {
            BufferedReader adjReader = new BufferedReader(new FileReader("/proc/" + String.valueOf(Pid) + "/oom_score_adj"));
            try {
                adj = Integer.parseInt(adjReader.readLine());
                adjReader.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.w(tag, "Check is java or native process Error:", e);
        }
        return adj;
    }

    public static boolean isEnabelPanicDThread(String tag) {
        return PANIC_D_THREAD && isDebugpolicyed(tag);
    }

    public static boolean isDebugpolicyed(String tag) {
        String dp = SystemProperties.get("ro.boot.dp", "unknown");
        if (dp.equals("0xB") || dp.equals("1") || dp.equals("2")) {
            Slog.i(tag, "this device has falshed Debugpolicy");
            return true;
        }
        Slog.i(tag, "this device didn't flash Debugpolicy");
        return false;
    }

    public static void doSysRqInterface(char c) {
        try {
            FileWriter sysrq_trigger = new FileWriter("/proc/sysrq-trigger");
            try {
                sysrq_trigger.write(c);
                sysrq_trigger.close();
            } finally {
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }

    public static void runCommand(String action, String params, int timeout) {
        int cmd_timeout;
        Slog.e(TAG, "runCommand action " + action + " params " + params);
        IMQSNative mClient = getmDaemon();
        if (mClient == null) {
            Slog.e(TAG, "runCommand no mqsasd!");
            return;
        }
        if (action == null || params == null) {
            Slog.e(TAG, "runCommand Wrong parameters!");
            return;
        }
        if (timeout < 60) {
            cmd_timeout = 60;
        } else {
            cmd_timeout = timeout;
        }
        try {
            int result = mClient.runCommand(action, params, cmd_timeout);
            if (result < 0) {
                Slog.e(TAG, "runCommanxd Fail result = " + result);
            }
            Slog.e(TAG, "runCommanxd result = " + result);
        } catch (Exception e) {
            Slog.e(TAG, "runCommanxd Exception " + e.toString());
            e.printStackTrace();
        }
    }

    public static void captureLog(String type, String headline, List<String> actions, List<String> params, boolean offline, int id, boolean upload, String where, List<String> includeFiles) {
        String where2;
        IMQSNative mClient = getmDaemon();
        if (mClient == null) {
            Slog.e(TAG, "CaptureLog no mqsasd!");
            return;
        }
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(headline)) {
            Slog.e(TAG, "CaptureLog type or headline is null!");
            return;
        }
        if (where == null) {
            Slog.d(TAG, "CaptureLog where is null!");
            where2 = "";
        } else {
            where2 = where;
        }
        try {
            mClient.captureLog(type, headline, actions, params, offline, id, upload, where2, includeFiles, false);
        } catch (RemoteException e) {
            Slog.e(TAG, "CaptureLog failed!", e);
        } catch (Exception e2) {
            Slog.e(TAG, "CaptureLog failed! unknown error", e2);
        }
    }

    public static String dumpOfflineLog(String reason, Action action, String type, String where) {
        try {
            SimpleDateFormat offlineLogDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
            String formattedDate = offlineLogDateFormat.format(new Date());
            String fileDir = where + File.separator;
            String fileName = formattedDate + d.h + reason;
            File offlineLogDir = new File(fileDir);
            if (offlineLogDir.exists() || offlineLogDir.mkdirs()) {
                Slog.e(TAG, "dumpOfflineLog reason:" + reason + " action=" + action + " type=" + type + " where=" + where);
                captureLog(type, fileName, action.actions, action.params, true, 1, false, fileDir, action.includeFiles);
                String zipPath = where + type + d.h + fileName + "_0.zip";
                return zipPath;
            }
            Slog.e(TAG, "Cannot create " + fileDir);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void copyRamoopsFileToMqs() {
        File ramoopsFile = new File(CONSOLE_RAMOOPS_PATH);
        File ramoops_0File = new File(CONSOLE_RAMOOPS_0_PATH);
        if ((ramoopsFile.exists() || ramoops_0File.exists()) && SystemProperties.getInt("sys.system_server.start_count", 1) == 1) {
            File lastKmsgFile = ramoopsFile.exists() ? ramoopsFile : ramoops_0File;
            try {
                FileOutputStream fos = new FileOutputStream(getMqsRamoopsFile());
                try {
                    FileInputStream ramoopsInput = new FileInputStream(lastKmsgFile);
                    try {
                        FileUtils.copy(ramoopsInput, fos);
                        ramoopsInput.close();
                        fos.close();
                    } finally {
                    }
                } finally {
                }
            } catch (IOException e) {
                Slog.w(TAG, "IOException: copyRamoopsFileToMqs fail");
                e.printStackTrace();
            } catch (Exception e2) {
                Slog.w(TAG, "UNKown Exception: copyRamoopsFileToMqs fail");
                e2.printStackTrace();
            }
        }
    }

    private static File getMqsRamoopsFile() {
        File mqsFsDir = new File(MQS_PSTORE_DIR);
        try {
            if (!mqsFsDir.exists() && !mqsFsDir.isDirectory()) {
                mqsFsDir.mkdirs();
                FileUtils.setPermissions(mqsFsDir, 508, -1, -1);
            }
            return new File(mqsFsDir.getAbsolutePath(), "console-ramoops");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static StackTraceElement[] getMiuiStackTraceByTid(int tid) {
        try {
            Class<?> clazz = Class.forName("dalvik.system.VMStack");
            Method method = clazz.getDeclaredMethod("getMiuiStackTraceByTid", Integer.TYPE);
            method.setAccessible(true);
            StackTraceElement[] st = (StackTraceElement[]) method.invoke(null, Integer.valueOf(tid));
            if (ENABLED_SCOUT_DEBUG) {
                Slog.d(TAG, "getMiuiStackTraceByTid tid:" + tid);
            }
            return st;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void dumpUithreadPeriodHistoryMessage(long anrTime, int duration) {
        try {
            MessageMonitor monitor = UiThread.get().getLooper().getMessageMonitor();
            if (monitor == null) {
                Slog.w("MIUIScout ANR", "Can't dumpPeriodHistoryMessage because of null MessageMonitor");
            } else {
                List<String> historymsg = monitor.getHistoryMsgInfoStringInPeriod(anrTime, duration);
                for (int i = 0; i < historymsg.size(); i++) {
                    Slog.d("MIUIScout ANR", "get period history msg from android.ui:" + historymsg.get(i));
                }
            }
        } catch (Exception e) {
            Slog.w("MIUIScout ANR", "AnrScout failed to get period history msg", e);
        }
        UiThread.get().getLooper().printLoopInfo(5);
    }

    public static String getProcessComm(int pid, int tid) {
        String comm = "unknow";
        if (tid == 0) {
            return "unknow";
        }
        if (pid == tid) {
            return getProcessCmdline(pid);
        }
        try {
            BufferedReader commReader = new BufferedReader(new FileReader("proc/" + tid + "/comm"));
            try {
                comm = commReader.readLine().trim();
                commReader.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.w(TAG, "Read process(" + tid + ") comm Error");
        }
        return comm;
    }

    public static String getProcessCmdline(int pid) {
        String cmdline = "unknow";
        if (pid == 0) {
            return "unknow";
        }
        try {
            BufferedReader cmdlineReader = new BufferedReader(new FileReader("proc/" + pid + "/cmdline"));
            try {
                cmdline = cmdlineReader.readLine().trim();
                cmdlineReader.close();
            } finally {
            }
        } catch (Exception e) {
            Slog.w(TAG, "Read process(" + pid + ") cmdline Error");
        }
        return cmdline;
    }

    public static ProcStatus getProcStatus(int Pid) {
        String[] strArr = STATUS_KEYS;
        long[] output = new long[strArr.length];
        output[0] = -1;
        Process.readProcLines("/proc/" + Pid + "/status", strArr, output);
        if (output[0] == -1) {
            return null;
        }
        ProcStatus procStatus = new ProcStatus();
        procStatus.tracerPid = (int) output[0];
        return procStatus;
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x008a  */
    /* JADX WARN: Removed duplicated region for block: B:22:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.String getPsInfo(int r11) {
        /*
            java.lang.String r0 = ""
            java.lang.String r1 = "close file stream error"
            java.lang.String r2 = "ScoutHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            switch(r11) {
                case 1: goto L11;
                default: goto Le;
            }
        Le:
            java.lang.String r4 = " -A"
            goto L14
        L11:
            java.lang.String r4 = " -AT"
        L14:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "ps"
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.StringBuilder r5 = r5.append(r4)
            java.lang.String r5 = r5.toString()
            r6 = 0
            r7 = 0
            java.lang.Runtime r8 = java.lang.Runtime.getRuntime()     // Catch: java.io.IOException -> Lab
            java.lang.Process r8 = r8.exec(r5)     // Catch: java.io.IOException -> Lab
            java.io.InputStreamReader r9 = new java.io.InputStreamReader     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            java.io.InputStream r10 = r8.getInputStream()     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r9.<init>(r10)     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r7 = r9
            java.io.BufferedReader r9 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r9.<init>(r7)     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r6 = r9
        L42:
            java.lang.String r9 = r6.readLine()     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r10 = r9
            if (r9 == 0) goto L52
            r3.append(r10)     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            r9 = 10
            r3.append(r9)     // Catch: java.lang.Throwable -> L60 java.io.IOException -> L62
            goto L42
        L52:
            r6.close()     // Catch: java.io.IOException -> L56
            goto L5a
        L56:
            r9 = move-exception
            android.util.Slog.e(r2, r1, r9)
        L5a:
            r7.close()     // Catch: java.io.IOException -> L5e
            goto L7f
        L5e:
            r9 = move-exception
            goto L7b
        L60:
            r0 = move-exception
            goto L8f
        L62:
            r9 = move-exception
            java.lang.String r10 = "io error when read stream"
            android.util.Slog.e(r2, r10, r9)     // Catch: java.lang.Throwable -> L60
            if (r6 == 0) goto L73
            r6.close()     // Catch: java.io.IOException -> L6e
            goto L73
        L6e:
            r9 = move-exception
            android.util.Slog.e(r2, r1, r9)
            goto L74
        L73:
        L74:
            if (r7 == 0) goto L7f
            r7.close()     // Catch: java.io.IOException -> L7a
            goto L7f
        L7a:
            r9 = move-exception
        L7b:
            android.util.Slog.e(r2, r1, r9)
            goto L80
        L7f:
        L80:
            r8.destroy()
            int r1 = r3.length()
            if (r1 <= 0) goto L8e
            java.lang.String r0 = r3.toString()
        L8e:
            return r0
        L8f:
            if (r6 == 0) goto L9a
            r6.close()     // Catch: java.io.IOException -> L95
            goto L9a
        L95:
            r9 = move-exception
            android.util.Slog.e(r2, r1, r9)
            goto L9b
        L9a:
        L9b:
            if (r7 == 0) goto La6
            r7.close()     // Catch: java.io.IOException -> La1
            goto La6
        La1:
            r9 = move-exception
            android.util.Slog.e(r2, r1, r9)
            goto La7
        La6:
        La7:
            r8.destroy()
            throw r0
        Lab:
            r1 = move-exception
            java.lang.String r8 = "can't exec the cmd "
            android.util.Slog.e(r2, r8, r1)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.ScoutHelper.getPsInfo(int):java.lang.String");
    }
}
