package com.android.server.am;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.NetworkBoost.NetworkSDK.ResultInfoConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class MimdManagerServiceImpl implements MimdManagerServiceStub {
    private static final long ACTION_CODE_SHIFT = 1000;
    private static final long ACTION_CODE_SLEEP = 1;
    public static final String ACTION_SLEEP_CHANGED = "com.miui.powerkeeper_sleep_changed";
    public static final String EXTRA_STATE = "state";
    private static final String MEMCG_PROCS_NODE = "cgroup.procs";
    private static final long POLICY_TYPE_APPBG = 3;
    private static final long POLICY_TYPE_APPDIE = 4;
    private static final long POLICY_TYPE_APPFG = 2;
    private static final long POLICY_TYPE_MISC = 1;
    private static final long POLICY_TYPE_NONE = 0;
    public static final int STATE_ENTER_SLEEP = 1;
    private static final long UID_CODE_SHIFT = 100000;
    private static final String UID_PREFIX = "uid_";
    IntentFilter filter;
    private IActivityManager mActivityManagerService;
    private Context mContext;
    private static String mAppMemCgroupPath = "/dev/memcg/mimd";
    private static final String TAG = "MimdManagerServiceImpl";
    private static final boolean DBG_MIMD = Log.isLoggable(TAG, 3);
    private static int mPolicyControlMaskLocal = 0;
    private static boolean sEnableMimdService = false;
    private final String MIMD_HWSERVICE_TRIGGER_PATH = "/sys/module/perf_helper/mimd/mimdtrigger";
    private final String MIMD_CONFIG_PATH = "/odm/etc/mimdconfig";
    private final Object mPolicyControlLock = new Object();
    private final Object mFgLock = new Object();
    private final Object mDieLock = new Object();
    private MimdManagerHandler mMimdManagerHandler = null;
    public HandlerThread mHandlerThread = null;
    private Map<String, AppMsg> appIndexMap = new HashMap();
    private BroadcastReceiver mSleepModeReceiver = new BroadcastReceiver() { // from class: com.android.server.am.MimdManagerServiceImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MimdManagerServiceImpl.ACTION_SLEEP_CHANGED.equals(action)) {
                int state = intent.getIntExtra("state", -1);
                if (1 == state) {
                    MimdManagerHandler mimdManagerHandler = MimdManagerServiceImpl.this.mMimdManagerHandler;
                    Objects.requireNonNull(MimdManagerServiceImpl.this.mMimdManagerHandler);
                    Message msg = mimdManagerHandler.obtainMessage(4);
                    MimdManagerServiceImpl.this.mMimdManagerHandler.sendMessage(msg);
                }
            }
        }
    };
    private final IProcessObserver.Stub mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.am.MimdManagerServiceImpl.2
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foreground) {
            AppInfo info = new AppInfo();
            info.pid = pid;
            info.uid = uid;
            info.foreground = foreground;
            MimdManagerHandler mimdManagerHandler = MimdManagerServiceImpl.this.mMimdManagerHandler;
            Objects.requireNonNull(MimdManagerServiceImpl.this.mMimdManagerHandler);
            Message msg = mimdManagerHandler.obtainMessage(2, info);
            MimdManagerServiceImpl.this.mMimdManagerHandler.sendMessage(msg);
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            AppInfo info = new AppInfo();
            info.pid = pid;
            info.uid = uid;
            MimdManagerHandler mimdManagerHandler = MimdManagerServiceImpl.this.mMimdManagerHandler;
            Objects.requireNonNull(MimdManagerServiceImpl.this.mMimdManagerHandler);
            Message msg = mimdManagerHandler.obtainMessage(3, info);
            MimdManagerServiceImpl.this.mMimdManagerHandler.sendMessage(msg);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MimdManagerServiceImpl> {

        /* compiled from: MimdManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MimdManagerServiceImpl INSTANCE = new MimdManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MimdManagerServiceImpl m518provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MimdManagerServiceImpl m517provideNewInstance() {
            return new MimdManagerServiceImpl();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AppMsg {
        int mAppIdx;
        int mPid;
        int mUid;
        private final Object mUpdatePid = new Object();

        public AppMsg(int idx, int pid) {
            this.mAppIdx = idx;
            this.mPid = pid;
        }

        public void UpdatePid(int pid) {
            synchronized (this.mUpdatePid) {
                this.mPid = pid;
            }
        }
    }

    /* loaded from: classes.dex */
    class AppInfo {
        boolean foreground;
        String packageName;
        int pid;
        int uid;

        AppInfo() {
        }
    }

    public void systemReady(Context context) {
        boolean z = SystemProperties.getBoolean("persist.sys.mimd.reclaim.enable", false);
        sEnableMimdService = z;
        this.mContext = context;
        if (!z) {
            Log.e(TAG, "All Features are disabled, do not create HandlerThread!");
            return;
        }
        HandlerThread handlerThread = new HandlerThread("mimd");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        MimdManagerHandler mimdManagerHandler = new MimdManagerHandler(this.mHandlerThread.getLooper());
        this.mMimdManagerHandler = mimdManagerHandler;
        Objects.requireNonNull(mimdManagerHandler);
        Message msg = mimdManagerHandler.obtainMessage(0);
        this.mMimdManagerHandler.sendMessage(msg);
        if (DBG_MIMD) {
            Log.d(TAG, "Create MimdManagerService succeed!");
        }
    }

    public void onProcessStart(int uid, int pid, String packageName) {
        if (sEnableMimdService) {
            AppInfo info = new AppInfo();
            info.pid = pid;
            info.uid = uid;
            info.packageName = packageName;
            MimdManagerHandler mimdManagerHandler = this.mMimdManagerHandler;
            Objects.requireNonNull(mimdManagerHandler);
            Message msg = mimdManagerHandler.obtainMessage(1, info);
            this.mMimdManagerHandler.sendMessage(msg);
        }
    }

    /* loaded from: classes.dex */
    private final class MimdManagerHandler extends Handler {
        final int MSG_APP_DIED;
        final int MSG_APP_FOREGROUND_CHANGE;
        final int MSG_MIMD_MANAGER_INIT;
        final int MSG_SLEEP_MODE_ENTRY;
        final int MSG_START_PROCESS;

        private MimdManagerHandler(Looper looper) {
            super(looper);
            this.MSG_MIMD_MANAGER_INIT = 0;
            this.MSG_START_PROCESS = 1;
            this.MSG_APP_FOREGROUND_CHANGE = 2;
            this.MSG_APP_DIED = 3;
            this.MSG_SLEEP_MODE_ENTRY = 4;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            AppInfo info = (AppInfo) msg.obj;
            switch (what) {
                case 0:
                    MimdManagerServiceImpl.this.mimdManagerInit();
                    return;
                case 1:
                    MimdManagerServiceImpl.this.onAppProcessStart(info.uid, info.pid, info.packageName);
                    return;
                case 2:
                    MimdManagerServiceImpl.this.foregroundChanged(info.pid, info.uid, info.foreground);
                    return;
                case 3:
                    MimdManagerServiceImpl.this.appDied(info.pid, info.uid);
                    return;
                case 4:
                    MimdManagerServiceImpl.this.entrySleepMode();
                    return;
                default:
                    Log.e(MimdManagerServiceImpl.TAG, "mimd manager service received wrong handler message:" + what);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void mimdManagerInit() {
        if (sEnableMimdService) {
            policyControlInit();
            boolean z = DBG_MIMD;
            if (z) {
                Log.d(TAG, "init per-app memcg reclaim succeed!");
            }
            IntentFilter intentFilter = new IntentFilter(ACTION_SLEEP_CHANGED);
            this.filter = intentFilter;
            this.mContext.registerReceiver(this.mSleepModeReceiver, intentFilter);
            if (z) {
                Log.d(TAG, "register sleep mode receiver succeed!");
            }
        }
    }

    private void policyControlInit() {
        File mTriggerFile = new File("/sys/module/perf_helper/mimd/mimdtrigger");
        if (mTriggerFile.exists()) {
            getPolicyControlMaskLocal();
            if (ParseMimdconfigAppmap()) {
                this.mActivityManagerService = ActivityManager.getService();
                registeForegroundReceiver();
                return;
            }
            return;
        }
        Log.e(TAG, "mimd trigger file not find!");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void entrySleepMode() {
        Trace.traceBegin(8L, "entry sleep");
        boolean res = PolicyControlTrigger(ResultInfoConstants.ERROR_LONG_CODE);
        if (DBG_MIMD && res) {
            Log.d(TAG, "enable sleep mode to reclaim memory!");
        }
        Trace.traceEnd(8L);
    }

    private void registeForegroundReceiver() {
        try {
            this.mActivityManagerService.registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            Log.e(TAG, "mimd manager service registerProcessObserver failed");
        }
    }

    private void getPolicyControlMaskLocal() {
        StringBuilder sb;
        File file = new File("/odm/etc/mimdconfig");
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String tmpString = reader.readLine();
                        if (tmpString != null) {
                            if (tmpString.lastIndexOf("PolicyControlMaskLocal:") >= 0) {
                                Pattern pattern = Pattern.compile("\\s+");
                                Matcher matcher = pattern.matcher(tmpString);
                                String tmpString2 = matcher.replaceAll(" ").trim();
                                int idx = tmpString2.lastIndexOf("PolicyControlMaskLocal:");
                                if (idx >= 0) {
                                    String pcmStr = tmpString2.substring(idx).trim();
                                    if (pcmStr.length() > 23) {
                                        Pattern patternNum = Pattern.compile("[0-9]*");
                                        Matcher isNum = patternNum.matcher(pcmStr.substring(23));
                                        if (isNum.matches()) {
                                            mPolicyControlMaskLocal = Integer.parseInt(pcmStr.substring(23));
                                        }
                                    }
                                }
                            }
                        } else {
                            try {
                                reader.close();
                                return;
                            } catch (IOException e) {
                                e = e;
                                sb = new StringBuilder();
                                Log.e(TAG, sb.append("read PolicyControlMaskLocal failed").append(e).toString());
                            }
                        }
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "read PolicyControlMaskLocal failed" + e2);
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                            e = e3;
                            sb = new StringBuilder();
                            Log.e(TAG, sb.append("read PolicyControlMaskLocal failed").append(e).toString());
                        }
                    }
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "read PolicyControlMaskLocal failed" + e4);
                    }
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void foregroundChanged(int pid, int uid, boolean foreground) {
        long tmpPT;
        synchronized (this.mFgLock) {
            String pkgName = getPackageNameByUid(uid);
            if (this.appIndexMap.containsKey(pkgName)) {
                Trace.traceBegin(8L, Integer.toString(pid) + " to fg " + Boolean.toString(foreground));
                AppMsg appMsg = this.appIndexMap.get(pkgName);
                if (appMsg.mPid == 0) {
                    appMsg.UpdatePid(pid);
                }
                if (appMsg.mUid != uid) {
                    removeEmptyMemcgDir(appMsg.mUid);
                    appMsg.mUid = uid;
                }
                int appidx = appMsg.mAppIdx;
                if (foreground) {
                    tmpPT = (appidx * 1000) + 2 + (uid * UID_CODE_SHIFT);
                    if (DBG_MIMD) {
                        Log.d(TAG, "Foreground changed, foreground pid=:" + pid + " Pkg=" + pkgName);
                    }
                } else {
                    tmpPT = (appidx * 1000) + POLICY_TYPE_APPBG + (uid * UID_CODE_SHIFT);
                    if (DBG_MIMD) {
                        Log.d(TAG, "Foreground changed, background pid=:" + pid + " Pkg=" + pkgName);
                    }
                }
                PolicyControlTrigger(tmpPT);
                Trace.traceEnd(8L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void appDied(int pid, int uid) {
        synchronized (this.mDieLock) {
            String pkgName = getPackageNameByUid(uid);
            if (this.appIndexMap.containsKey(pkgName)) {
                AppMsg appMsg = this.appIndexMap.get(pkgName);
                if (appMsg.mPid > 0 && appMsg.mPid == pid) {
                    Trace.traceBegin(8L, Integer.toString(pid) + " died");
                    int appidx = appMsg.mAppIdx;
                    appMsg.UpdatePid(0);
                    long tmpPT = (appidx * 1000) + 4 + (uid * UID_CODE_SHIFT);
                    PolicyControlTrigger(tmpPT);
                    Log.d(TAG, "ProcessDied, pid=:" + pid + " uid=" + uid + " type=" + tmpPT);
                    Trace.traceEnd(8L);
                }
            }
        }
    }

    private String getProcessNameByPid(int pid) {
        String processName = ProcessUtils.getProcessNameByPid(pid);
        return processName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppProcessStart(int uid, int pid, String pkg) {
        if (this.appIndexMap.containsKey(getProcessNameByPid(pid)) && checkAppCgroup(uid)) {
            String uidPath = mAppMemCgroupPath + "/" + UID_PREFIX + uid;
            writeToSys(uidPath + "/" + MEMCG_PROCS_NODE, Integer.toString(pid));
        }
    }

    private boolean checkAppCgroup(int uid) {
        String uidPath = mAppMemCgroupPath + "/" + UID_PREFIX + uid;
        File appCgDir = new File(uidPath);
        if (appCgDir.exists()) {
            return true;
        }
        if (appCgDir.mkdir()) {
            appCgDir.setReadable(true, false);
            appCgDir.setWritable(true, true);
            appCgDir.setExecutable(true, false);
            if (DBG_MIMD) {
                Log.d(TAG, "create app cgroup succeed, path:" + uidPath);
            }
            return true;
        }
        Log.e(TAG, "create app cgroup failed, path:" + uidPath);
        return false;
    }

    private static void deleteFolder(File folder) {
        File[] files;
        if (folder.isDirectory() && (files = folder.listFiles()) != null) {
            for (File file : files) {
                deleteFolder(file);
            }
        }
        folder.delete();
    }

    private static void removeEmptyMemcgDir(int uid) {
        String uidPath = mAppMemCgroupPath + "/" + UID_PREFIX + uid;
        File appCgDir = new File(uidPath);
        if (appCgDir.exists()) {
            deleteFolder(appCgDir);
            if (DBG_MIMD) {
                Log.d(TAG, "remove app cgroup succeed,path:" + uidPath);
            }
        }
    }

    private static void writeToSys(String path, String content) {
        StringBuilder sb;
        File file = new File(path);
        FileWriter writer = null;
        try {
            if (file.exists()) {
                try {
                    writer = new FileWriter(path);
                    writer.write(content);
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e = e;
                        sb = new StringBuilder();
                        Log.e(TAG, sb.append("close path failed, err:").append(e).toString());
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "open or write failed, err:" + e2);
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e3) {
                            e = e3;
                            sb = new StringBuilder();
                            Log.e(TAG, sb.append("close path failed, err:").append(e).toString());
                        }
                    }
                }
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e4) {
                    Log.e(TAG, "close path failed, err:" + e4);
                }
            }
            throw th;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x01ca  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x020d  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0191 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:54:0x01a2 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:61:? A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean ParseMimdconfigAppmap() {
        /*
            Method dump skipped, instructions count: 528
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.MimdManagerServiceImpl.ParseMimdconfigAppmap():boolean");
    }

    private String getPackageNameByUid(int uid) {
        String[] pkgs = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (pkgs != null && pkgs.length > 0) {
            String packageName = pkgs[0];
            return packageName;
        }
        String packageName2 = Integer.toString(uid);
        return packageName2;
    }

    private boolean PolicyControlTrigger(long mPolicyType) {
        synchronized (this.mPolicyControlLock) {
            int i = (int) (mPolicyType % 1000);
            int mTmpMask = mPolicyControlMaskLocal;
            if (mPolicyType == 0) {
                return false;
            }
            while (true) {
                int i2 = i - 1;
                if (i == 0) {
                    break;
                }
                mTmpMask /= 10;
                i = i2;
            }
            int mIsTrigger = mTmpMask % 10;
            if (mIsTrigger == 0) {
                return false;
            }
            TriggerPolicy(mPolicyType);
            return true;
        }
    }

    private void TriggerPolicy(long mPolicyType) {
        StringBuilder sb;
        File file = new File("/sys/module/perf_helper/mimd/mimdtrigger");
        if (file.exists()) {
            FileOutputStream fos = null;
            try {
                try {
                    fos = new FileOutputStream(file);
                    fos.write(Long.toString(mPolicyType).getBytes());
                    if (DBG_MIMD) {
                        Log.d(TAG, "write mimdtrigger string " + mPolicyType);
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e = e;
                        sb = new StringBuilder();
                        Log.e(TAG, sb.append("close mimdtrigger failed").append(e).toString());
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "write mimdtrigger failed" + e2);
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e3) {
                            e = e3;
                            sb = new StringBuilder();
                            Log.e(TAG, sb.append("close mimdtrigger failed").append(e).toString());
                        }
                    }
                }
            } catch (Throwable th) {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "close mimdtrigger failed" + e4);
                    }
                }
                throw th;
            }
        }
    }
}
