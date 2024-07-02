package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.ScoutSystemMonitor;
import com.android.server.UiThread;
import com.android.server.am.MiuiWarnings;
import com.android.server.am.ProcessRecord;
import com.android.server.am.ProcessUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.GeneralExceptionEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiWindowMonitorImpl extends MiuiWindowMonitorStub {
    private static final String APP_ID = "APP_ID";
    private static final String CONTENT = "content";
    private static final int DEFAULT_EXIT_REPORT_THRESHOLD = 50;
    private static final int DEFAULT_KILL_THRESHOLD = 200;
    private static final int DEFAULT_REPORT_INTERVAL = 20;
    private static final int DEFAULT_TOTAL_WINDOW_THRESHOLD = 3891;
    private static final int DEFAULT_WARN_INTERVAL = 20;
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String EXIT_REPORT_THRESHOLD_PROPERTY = "persist.sys.stability.window_monitor.exit_report_threshold";
    private static final String EXTRA_APP_ID = "31000401516";
    private static final String EXTRA_EVENT_NAME = "app_resource_info";
    private static final String EXTRA_PACKAGE_NAME = "android";
    private static final int FLAG_MQS_REPORT = 2;
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final int FLAG_ONETRACK_REPORT = 1;
    private static final List<String> FOREGROUND_PERSISTENT_APPS = Arrays.asList(AccessController.PACKAGE_SYSTEMUI, "com.android.phone", "com.android.nfc", "com.miui.voicetrigger");
    private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
    private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
    private static final String KEY_TOP_TYPE_WINDOWS = "key_top_type_windows";
    private static final String KILL_REASON = "window_leak_monitor";
    private static final String KILL_THRESHOLD_PROPERTY = "persist.sys.stability.window_monitor.kill_threshold";
    private static final String KILL_WHEN_LEAK_PROPERTY = "persist.sys.stability.window_monitor.kill_when_leak";
    private static final int LEAK_TYPE_APPLICATION = 1;
    private static final int LEAK_TYPE_NONE = 0;
    private static final int LEAK_TYPE_SYSTEM = 2;
    private static final String PACKAGE = "PACKAGE";
    private static final String REPORT_INTERVAL_PROPERTY = "persist.sys.stability.window_monitor.report_interval";
    private static final String SWITCH_PROPERTY = "persist.sys.stability.window_monitor.enabled";
    private static final String TAG = "MiuiWindowMonitorImpl";
    private static final String TOTAL_WINDOW_THRESHOLD_PROPERTY = "persist.sys.stability.window_monitor.total_window_threshold";
    private static final String WARN_INTERVAL_PROPERTY = "persist.sys.stability.window_monitor.warn_interval";
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mMonitorThread;
    private WindowManagerService mService;
    private HashMap<IBinder, WindowInfo> mWindowsByToken = new HashMap<>();
    private final HashMap<ProcessKey, ProcessWindowList> mWindowsByApp = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiWindowMonitorImpl> {

        /* compiled from: MiuiWindowMonitorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiWindowMonitorImpl INSTANCE = new MiuiWindowMonitorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiWindowMonitorImpl m2694provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiWindowMonitorImpl m2693provideNewInstance() {
            return new MiuiWindowMonitorImpl();
        }
    }

    public void init(Context context, WindowManagerService service) {
        this.mService = service;
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("window-leak-monitor-thread");
        this.mMonitorThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mMonitorThread.getLooper());
    }

    public boolean addWindowInfoLocked(final int pid, final String packageName, final IBinder token, final String windowName, final int type) {
        if (pid < 0 || token == null || TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "invalid data: pid=" + pid + ", token=" + token + ", packageName=" + packageName);
            return false;
        }
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    MiuiWindowMonitorImpl.this.addWindowInfo(pid, packageName, token, windowName, type);
                }
            });
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void addWindowInfo(int pid, String packageName, IBinder token, String windowName, int type) {
        ProcessKey key = new ProcessKey(pid, packageName);
        WindowInfo info = new WindowInfo(pid, packageName, token, windowName, type);
        this.mWindowsByToken.put(token, info);
        ProcessWindowList processWindows = this.mWindowsByApp.get(key);
        if (processWindows == null) {
            processWindows = new ProcessWindowList(pid, packageName);
            this.mWindowsByApp.put(key, processWindows);
        }
        processWindows.put(token, info);
        if (isEnabled()) {
            checkProcess(processWindows);
        }
    }

    private void checkProcess(ProcessWindowList processWindows) {
        if (processWindows.warned() || !this.mWindowsByApp.containsValue(processWindows)) {
            return;
        }
        ProcessWindowList leakProcess = anyProcessLeak(processWindows);
        if (leakProcess != null) {
            dealWithLeakProcess(leakProcess.getPid(), leakProcess);
        } else if (!processWindows.warned()) {
            processWindows.setWarned(false);
            processWindows.setWillKill(false);
        }
    }

    private boolean dealWithLeakProcess(int pid, ProcessWindowList processWindows) {
        if (processWindows.getPid() == Process.myPid()) {
            processWindows.setWarned(false);
            processWindows.setWillKill(false);
            reportIfNeeded(pid, processWindows, false);
            return false;
        }
        if (!killByPolicy() && processWindows.getLeakType() == 1) {
            processWindows.setWarned(false);
            processWindows.setWillKill(false);
            reportIfNeeded(pid, processWindows, false);
            return false;
        }
        String pkgName = processWindows.getPackageName();
        boolean persistent = ProcessUtils.isPersistent(this.mContext, pkgName);
        if ((ProcessUtils.getCurAdjByPid(pid) <= DEFAULT_KILL_THRESHOLD && !persistent) || FOREGROUND_PERSISTENT_APPS.contains(pkgName)) {
            int curCount = processWindows.getCurrentCount();
            if (curCount - processWindows.getLastWarnCount() >= getWarnInterval()) {
                processWindows.setWarned(true);
                processWindows.setLastWarnCount(curCount);
                showKillAppDialog(processWindows, pkgName, persistent);
            } else {
                processWindows.setWarned(false);
                processWindows.setWillKill(false);
                reportIfNeeded(pid, processWindows, false);
            }
            return false;
        }
        processWindows.setWarned(false);
        processWindows.setWillKill(true);
        reportIfNeeded(pid, processWindows, false);
        killApp(pid);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void killApp(int pid) {
        Slog.w(TAG, "kill proc " + pid + " because it creates too many windows!");
        Process.killProcess(pid);
    }

    private void showKillAppDialog(final ProcessWindowList processWindows, final String pkgName, final boolean persistent) {
        final int pid = processWindows.getPid();
        final String appLabel = ProcessUtils.getApplicationLabel(this.mContext, pkgName);
        final MiuiWarnings.WarningCallback callback = new MiuiWarnings.WarningCallback() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.2
            @Override // com.android.server.am.MiuiWarnings.WarningCallback
            public void onCallback(boolean positive) {
                synchronized (MiuiWindowMonitorImpl.this) {
                    if (!positive) {
                        processWindows.setWillKill(false);
                    } else {
                        processWindows.setWillKill(true);
                    }
                    MiuiWindowMonitorImpl.this.reportIfNeeded(pid, processWindows, false);
                    if (positive) {
                        Slog.w(MiuiWindowMonitorImpl.TAG, "kill to avoid window leak, pid=" + pid + ", package=" + pkgName + ", label=" + appLabel + ", persistent=" + persistent);
                        MiuiWindowMonitorImpl.this.killApp(pid);
                    } else {
                        Slog.w(MiuiWindowMonitorImpl.TAG, "application create too many windows, pid=" + pid + ", package=" + pkgName + ", label=" + appLabel + ", persistent=" + persistent);
                        processWindows.setWillKill(false);
                    }
                    processWindows.setWarned(false);
                }
            }
        };
        UiThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.3
            @Override // java.lang.Runnable
            public void run() {
                MiuiWarnings.getInstance().showWarningDialog(appLabel, callback);
            }
        });
    }

    public void removeWindowInfoLocked(final IBinder token) {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    MiuiWindowMonitorImpl.this.removeWindowInfo(token);
                }
            });
        }
    }

    public void onProcessDiedLocked(final int pid, ProcessRecord app) {
        final ProcessKey key = new ProcessKey(pid, ProcessUtils.getPackageNameByApp(app));
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (MiuiWindowMonitorImpl.this) {
                        if (!MiuiWindowMonitorImpl.this.isEnabled()) {
                            MiuiWindowMonitorImpl.this.removeWindowInfo(key);
                        } else {
                            MiuiWindowMonitorImpl miuiWindowMonitorImpl = MiuiWindowMonitorImpl.this;
                            miuiWindowMonitorImpl.reportIfNeeded(pid, (ProcessWindowList) miuiWindowMonitorImpl.mWindowsByApp.get(key), true);
                        }
                    }
                }
            });
        }
    }

    public synchronized int dump(PrintWriter pw) {
        if (pw == null) {
            return 0;
        }
        pw.println("DUMP MiuiWindowMonitor INFO");
        pw.println();
        pw.print("enabled=");
        pw.println(isEnabled());
        pw.print("kill_when_leak=");
        pw.println(killByPolicy());
        pw.print("kill_threshold=");
        pw.println(getKillThreshold());
        pw.print("exit_report_threshold=");
        pw.println(getExitReportThreshold());
        pw.print("warn_interval=");
        pw.println(getWarnInterval());
        pw.print("report_interval=");
        pw.println(getReportInterval());
        pw.print("total_window_threshold=");
        pw.println(getTotalWindowThreshold());
        pw.println();
        int procCount = this.mWindowsByApp.size();
        if (procCount == 0) {
            pw.println("no process has windows");
            return 0;
        }
        pw.println();
        int index = 0;
        int windowCount = 0;
        for (ProcessWindowList pwl : this.mWindowsByApp.values()) {
            if (pwl != null) {
                pw.print("  Process#");
                pw.print(index);
                pw.print(" ");
                pw.println(pwl.toString());
                pw.println();
                windowCount += pwl.getCurrentCount();
                index++;
            }
        }
        pw.print("TOTAL:");
        pw.print(procCount);
        pw.print(" processes, ");
        pw.print(windowCount);
        pw.println(" windows");
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEnabled() {
        return SystemProperties.getBoolean(SWITCH_PROPERTY, false);
    }

    private boolean killByPolicy() {
        return SystemProperties.getBoolean(KILL_WHEN_LEAK_PROPERTY, false);
    }

    private int getKillThreshold() {
        return SystemProperties.getInt(KILL_THRESHOLD_PROPERTY, DEFAULT_KILL_THRESHOLD);
    }

    private int getExitReportThreshold() {
        return SystemProperties.getInt(EXIT_REPORT_THRESHOLD_PROPERTY, 50);
    }

    private int getReportInterval() {
        return SystemProperties.getInt(REPORT_INTERVAL_PROPERTY, 20);
    }

    private int getWarnInterval() {
        return SystemProperties.getInt(WARN_INTERVAL_PROPERTY, 20);
    }

    private int getTotalWindowThreshold() {
        return SystemProperties.getInt(TOTAL_WINDOW_THRESHOLD_PROPERTY, DEFAULT_TOTAL_WINDOW_THRESHOLD);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void removeWindowInfo(IBinder token) {
        ProcessKey key;
        ProcessWindowList processWindows;
        WindowInfo info = this.mWindowsByToken.remove(token);
        if (info != null && (processWindows = this.mWindowsByApp.get((key = new ProcessKey(info.mPid, info.mPackageName)))) != null) {
            processWindows.remove(info.mType, info.mWindowName, token);
            if (processWindows.getCurrentCount() <= 0) {
                this.mWindowsByApp.remove(key);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ProcessWindowList removeWindowInfo(ProcessKey key) {
        ProcessWindowList processWindows = this.mWindowsByApp.remove(key);
        if (processWindows == null) {
            return null;
        }
        SparseArray<HashMap<String, HashMap<IBinder, WindowInfo>>> windowsByType = processWindows.getWindows();
        int size = windowsByType.size();
        for (int i = 0; i < size; i++) {
            HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = windowsByType.valueAt(i);
            if (windowsByName != null && windowsByName.size() > 0) {
                for (HashMap<IBinder, WindowInfo> windows : windowsByName.values()) {
                    if (windows != null && windows.size() > 0) {
                        for (IBinder token : windows.keySet()) {
                            this.mWindowsByToken.remove(token);
                        }
                    }
                }
            }
        }
        return processWindows;
    }

    private ProcessWindowList selectTopProcess() {
        ProcessWindowList most = null;
        int count = -1;
        for (ProcessWindowList pl : this.mWindowsByApp.values()) {
            int plCount = pl.getCurrentCount();
            if (plCount > count) {
                count = plCount;
                most = pl;
            }
        }
        return most;
    }

    private ProcessWindowList anyProcessLeak(ProcessWindowList processWindows) {
        ProcessWindowList most;
        if (this.mWindowsByToken.size() >= getTotalWindowThreshold() && (most = selectTopProcess()) != null) {
            most.setLeakType(2);
            return most;
        }
        if (processWindows == null) {
            return null;
        }
        if (processWindows.getCurrentCount() >= getKillThreshold()) {
            processWindows.setLeakType(1);
            return processWindows;
        }
        processWindows.setLeakType(0);
        return null;
    }

    private int shouldReport(int pid, ProcessWindowList processWindows, boolean died) {
        if (pid < 0 || processWindows == null) {
            return 0;
        }
        int curCount = processWindows.getCurrentCount();
        if (curCount == 0) {
            processWindows.setLastReportCount(0);
            return 0;
        }
        if (died) {
            if (processWindows.getLeakType() != 0 || curCount <= getExitReportThreshold()) {
                return 0;
            }
            int report = 0 | 1;
            return report;
        }
        if (processWindows.getLeakType() == 0 || curCount - processWindows.getLastReportCount() < getReportInterval()) {
            return 0;
        }
        int report2 = 0 | 3;
        processWindows.setLastReportCount(curCount);
        return report2;
    }

    private void doOneTrackReport(String content) {
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra("APP_ID", EXTRA_APP_ID);
            intent.putExtra("EVENT_NAME", EXTRA_EVENT_NAME);
            intent.putExtra("PACKAGE", EXTRA_PACKAGE_NAME);
            Bundle params = new Bundle();
            params.putString(CONTENT, content);
            intent.putExtras(params);
            intent.setFlags(3);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doMqsReport(int pid, String packageName, String details, String topTypeWindows) {
        GeneralExceptionEvent event = new GeneralExceptionEvent();
        event.setType(423);
        event.setPid(pid);
        event.setPackageName(packageName);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary("window leaked! package=" + packageName + ", pid=" + pid);
        event.setDetails(details);
        Bundle bundle = event.getBundle();
        bundle.putString(KEY_TOP_TYPE_WINDOWS, topTypeWindows);
        MQSEventManagerDelegate.getInstance().reportGeneralException(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportWindowInfo(int pid, String packageName, String infoStr, String topTypeWindows, int report) {
        if ((report & 1) != 0) {
            Slog.i(TAG, "doOneTrackReport:" + infoStr);
            doOneTrackReport(infoStr);
        }
        if ((report & 2) != 0) {
            Slog.i(TAG, "doMqsReport:" + topTypeWindows);
            doMqsReport(pid, packageName, infoStr, topTypeWindows);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportIfNeeded(int pid, ProcessWindowList processWindows, boolean died) {
        final int report = shouldReport(pid, processWindows, died);
        if (died && processWindows != null) {
            ProcessKey key = new ProcessKey(pid, processWindows.getPackageName());
            removeWindowInfo(key);
        }
        if (report == 0) {
            return;
        }
        final String infoStr = processWindows.toJson().toString();
        if (TextUtils.isEmpty(infoStr)) {
            Slog.w(TAG, "app:" + processWindows.toString() + ", to json string error!");
            return;
        }
        final String topTypeWindows = processWindows.getTopTypeWindows();
        final int reportedPid = processWindows.getPid();
        final String reportedPkg = processWindows.getPackageName();
        Handler handler = ScoutSystemMonitor.getInstance().getSystemWorkerHandler();
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.MiuiWindowMonitorImpl.6
                @Override // java.lang.Runnable
                public void run() {
                    MiuiWindowMonitorImpl.this.reportWindowInfo(reportedPid, reportedPkg, infoStr, topTypeWindows, report);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class WindowInfo {
        private static final String KEY_NAME = "name";
        private static final String KEY_TYPE = "type";
        String mPackageName;
        int mPid;
        IBinder mToken;
        int mType;
        String mWindowName;

        public WindowInfo(int pid, String packageName, IBinder token, String windowName, int type) {
            this.mPid = pid;
            this.mPackageName = packageName;
            this.mToken = token;
            this.mWindowName = windowName;
            this.mType = type;
        }

        public String toString() {
            return this.mWindowName + "|" + this.mType;
        }

        public JSONObject toJson() {
            JSONObject jobj = new JSONObject();
            try {
                jobj.put(KEY_NAME, this.mWindowName);
                return jobj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ProcessKey {
        String mPackageName;
        int mPid;

        public ProcessKey(int pid, String packageName) {
            this.mPid = pid;
            this.mPackageName = packageName;
        }

        public boolean equals(ProcessKey that) {
            if (that == null) {
                return false;
            }
            if (this == that) {
                return true;
            }
            if (this.mPid != that.mPid || !TextUtils.equals(this.mPackageName, that.mPackageName)) {
                return false;
            }
            return true;
        }

        public boolean equals(Object obj) {
            try {
                return equals((ProcessKey) obj);
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return (this.mPackageName + this.mPid).hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ProcessWindowList {
        private static final String KEY_COUNT = "count";
        private static final String KEY_CURRENT_COUNT = "curCount";
        private static final String KEY_KILLED = "killed";
        private static final String KEY_LEAK_TYPE = "leakType";
        private static final String KEY_MAXCOUNT = "maxCount";
        private static final String KEY_PACKAGE = "package";
        private static final String KEY_PID = "pid";
        private static final String KEY_RESOURCE_TYPE = "resourceType";
        private static final String KEY_TYPE = "type";
        private static final String KEY_TYPES = "types";
        private static final String KEY_WARNED = "warned";
        private static final String KEY_WINDOWS = "windows";
        private static final String[] LEAK_TYPES = {"none", "application", "system"};
        private static final String RESOURCE_TYPE = "window";
        private int mCurCount;
        private int mLastReportCount;
        private int mLastWarnCount;
        private int mMaxCount;
        private String mPackageName;
        private int mPid;
        private SparseArray<HashMap<String, HashMap<IBinder, WindowInfo>>> mWindowsByType = new SparseArray<>();
        private int mLeakType = 0;
        private boolean mWarned = false;
        private boolean mWillKill = false;

        public ProcessWindowList(int pid, String packageName) {
            this.mPid = pid;
            this.mPackageName = packageName;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("pid=");
            builder.append(this.mPid).append(",").append("package=").append(this.mPackageName).append("\n");
            int size = this.mWindowsByType.size();
            for (int i = 0; i < size; i++) {
                HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = this.mWindowsByType.valueAt(i);
                int windowType = this.mWindowsByType.keyAt(i);
                if (windowsByName != null && windowsByName.size() > 0) {
                    builder.append("    Type:").append(windowType).append(",count=").append(windowsByName.size()).append("\n");
                    for (Map.Entry<String, HashMap<IBinder, WindowInfo>> windows : windowsByName.entrySet()) {
                        String name = windows.getKey();
                        HashMap<IBinder, WindowInfo> value = windows.getValue();
                        int nameCount = 0;
                        if (value != null) {
                            nameCount = value.size();
                        }
                        if (nameCount > 0) {
                            builder.append("      Windows:").append(name).append(",count=").append(nameCount).append("\n");
                        }
                    }
                }
            }
            builder.append("  ").append("currentCount=").append(getCurrentCount()).append(",").append(KEY_MAXCOUNT).append("=").append(this.mMaxCount);
            return builder.toString();
        }

        JSONObject toJson() {
            JSONObject jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            try {
                jSONObject.put(KEY_RESOURCE_TYPE, "window");
                jSONObject.put(KEY_PID, this.mPid);
                jSONObject.put(KEY_PACKAGE, this.mPackageName);
                jSONObject.put(KEY_MAXCOUNT, this.mMaxCount);
                jSONObject.put(KEY_LEAK_TYPE, LEAK_TYPES[this.mLeakType]);
                jSONObject.put(KEY_WARNED, this.mWarned);
                jSONObject.put(KEY_KILLED, this.mWillKill);
                int size = this.mWindowsByType.size();
                for (int i = 0; i < size; i++) {
                    HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = this.mWindowsByType.valueAt(i);
                    int windowType = this.mWindowsByType.keyAt(i);
                    if (windowsByName != null && windowsByName.size() > 0) {
                        int typeCount = 0;
                        JSONObject typeObj = new JSONObject();
                        JSONObject nameObj = new JSONObject();
                        for (Map.Entry<String, HashMap<IBinder, WindowInfo>> windows : windowsByName.entrySet()) {
                            String name = windows.getKey();
                            HashMap<IBinder, WindowInfo> value = windows.getValue();
                            int nameCount = 0;
                            if (value != null) {
                                nameCount = value.size();
                            }
                            if (nameCount > 0) {
                                typeCount += nameCount;
                                nameObj.put(name, nameCount);
                            }
                        }
                        if (typeCount > 0) {
                            typeObj.put("type", windowType);
                            typeObj.put(KEY_COUNT, typeCount);
                            typeObj.put(KEY_WINDOWS, nameObj);
                            jSONArray.put(typeObj);
                        }
                    }
                }
                jSONObject.put(KEY_CURRENT_COUNT, getCurrentCount());
                if (getCurrentCount() > 0) {
                    jSONObject.put(KEY_TYPES, jSONArray);
                }
                return jSONObject;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        int getMaxCount() {
            return this.mMaxCount;
        }

        int getCurrentCount() {
            return this.mCurCount;
        }

        int getPid() {
            return this.mPid;
        }

        String getPackageName() {
            return this.mPackageName;
        }

        int getLastReportCount() {
            return this.mLastReportCount;
        }

        void setLastReportCount(int lastReportCount) {
            this.mLastReportCount = lastReportCount;
        }

        int getLastWarnCount() {
            return this.mLastWarnCount;
        }

        void setLastWarnCount(int lastWarnCount) {
            this.mLastWarnCount = lastWarnCount;
        }

        void setLeakType(int leakType) {
            this.mLeakType = leakType;
        }

        int getLeakType() {
            return this.mLeakType;
        }

        void setWarned(boolean warned) {
            this.mWarned = warned;
        }

        boolean warned() {
            return this.mWarned;
        }

        void setWillKill(boolean willKill) {
            this.mWillKill = willKill;
        }

        SparseArray<HashMap<String, HashMap<IBinder, WindowInfo>>> getWindows() {
            return this.mWindowsByType;
        }

        String getTopTypeWindows() {
            HashMap<String, HashMap<IBinder, WindowInfo>> top = null;
            int topCount = -1;
            int size = this.mWindowsByType.size();
            for (int i = 0; i < size; i++) {
                HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = this.mWindowsByType.valueAt(i);
                if (windowsByName != null && windowsByName.size() > 0) {
                    int typeCount = 0;
                    for (HashMap<IBinder, WindowInfo> windows : windowsByName.values()) {
                        if (windows != null && windows.size() > 0) {
                            typeCount += windows.size();
                        }
                    }
                    if (typeCount > topCount) {
                        topCount = typeCount;
                        top = windowsByName;
                        this.mWindowsByType.keyAt(i);
                    }
                }
            }
            if (top == null || top.size() <= 0) {
                return "no window";
            }
            StringBuilder result = new StringBuilder();
            for (String name : top.keySet()) {
                if (!TextUtils.isEmpty(name)) {
                    result.append(name).append("|");
                }
            }
            return result.toString();
        }

        void put(IBinder token, WindowInfo info) {
            if (this.mPid != info.mPid || !TextUtils.equals(this.mPackageName, info.mPackageName)) {
                Slog.w(MiuiWindowMonitorImpl.TAG, "pid not match, mPid=" + this.mPid + ", info.pid=" + info.mPid + ", mPackage=" + this.mPackageName + ", info.package=" + info.mPackageName + ", info.window=" + info.mWindowName + ", info.type=" + info.mType);
                return;
            }
            HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = this.mWindowsByType.get(info.mType);
            if (windowsByName == null) {
                windowsByName = new HashMap<>();
                this.mWindowsByType.put(info.mType, windowsByName);
            }
            HashMap<IBinder, WindowInfo> windows = windowsByName.get(info.mWindowName);
            if (windows == null) {
                windows = new HashMap<>();
                windowsByName.put(info.mWindowName, windows);
            }
            windows.put(token, info);
            this.mCurCount++;
            int count = getCurrentCount();
            if (count > this.mMaxCount) {
                this.mMaxCount = count;
            }
        }

        void remove(int type, String name, IBinder token) {
            HashMap<IBinder, WindowInfo> windows;
            HashMap<String, HashMap<IBinder, WindowInfo>> windowsByName = this.mWindowsByType.get(type);
            if (windowsByName == null || (windows = windowsByName.get(name)) == null) {
                return;
            }
            windows.remove(token);
            this.mCurCount--;
            if (windows.size() == 0) {
                windowsByName.remove(name);
            }
            if (windowsByName.size() == 0) {
                this.mWindowsByType.remove(type);
            }
        }
    }
}
