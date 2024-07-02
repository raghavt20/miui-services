package com.android.server.wm;

import android.os.MiPerf;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: classes.dex */
public class MiuiMiPerfStubImpl implements MiuiMiPerfStub {
    private static final String DEFAULT_COMMON_PACKAGENAME = "default.common.packagename";
    private static final int PROFILE_MAX_BYTE = 1048576;
    private static final String TAG = "MiuiMiPerfStubImpl";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";
    private static HashMap<String, HashMap<String, String>> mMiperfXmlMap = MiPerf.miPerfGetXmlMap();
    private static HashMap<String, String> mThermalSconfigMap = MiPerf.getThermalMap();
    private Timer timer;
    private TimerTask timerTask;
    private boolean isRecoverWrite = false;
    private boolean isRelease = false;
    private boolean isCommon = false;
    private boolean isdefault = false;
    private boolean isDebug = false;
    private String lastPackName = " ";
    private String lastActName = " ";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMiPerfStubImpl> {

        /* compiled from: MiuiMiPerfStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMiPerfStubImpl INSTANCE = new MiuiMiPerfStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiMiPerfStubImpl m2557provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiMiPerfStubImpl m2556provideNewInstance() {
            return new MiuiMiPerfStubImpl();
        }
    }

    public MiuiMiPerfStubImpl() {
        Slog.d(TAG, "MiuiMiPerfStubImpl is Initialized!");
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0070  */
    /* JADX WARN: Removed duplicated region for block: B:8:0x006d A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String readFile(java.lang.String r9) {
        /*
            java.lang.String r0 = "readFromFile, finally fis.close() IOException e:"
            java.lang.String r1 = "MiuiMiPerfStubImpl"
            r2 = 0
            r3 = 0
            r4 = 8
            char[] r5 = new char[r4]
            java.io.BufferedReader r6 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            java.io.FileReader r7 = new java.io.FileReader     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            r7.<init>(r9)     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            r6.<init>(r7)     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            r3 = r6
            r3.read(r5)     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            r3.close()     // Catch: java.lang.Throwable -> L3b java.io.IOException -> L3d
            r3.close()     // Catch: java.io.IOException -> L20
            goto L3a
        L20:
            r6 = move-exception
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
        L26:
            java.lang.StringBuilder r0 = r7.append(r0)
            java.lang.String r7 = r6.getMessage()
            java.lang.StringBuilder r0 = r0.append(r7)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
            r2 = 0
        L3a:
            goto L66
        L3b:
            r6 = move-exception
            goto La0
        L3d:
            r6 = move-exception
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3b
            r7.<init>()     // Catch: java.lang.Throwable -> L3b
            java.lang.String r8 = "readFile, IOException e:"
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.lang.Throwable -> L3b
            java.lang.String r8 = r6.getMessage()     // Catch: java.lang.Throwable -> L3b
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.lang.Throwable -> L3b
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Throwable -> L3b
            android.util.Slog.e(r1, r7)     // Catch: java.lang.Throwable -> L3b
            r2 = 0
            if (r3 == 0) goto L66
            r3.close()     // Catch: java.io.IOException -> L5f
            goto L3a
        L5f:
            r6 = move-exception
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            goto L26
        L66:
            r0 = 0
            char r1 = r5[r0]
            r6 = 45
            if (r1 != r6) goto L70
            java.lang.String r0 = "-1"
            return r0
        L70:
            char r0 = r5[r0]
            java.lang.String r0 = java.lang.String.valueOf(r0)
            r1 = 1
        L77:
            if (r1 >= r4) goto L9f
            char r2 = r5[r1]
            r6 = 48
            if (r2 < r6) goto L9f
            char r2 = r5[r1]
            r6 = 57
            if (r2 > r6) goto L9f
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r0)
            char r6 = r5[r1]
            java.lang.String r6 = java.lang.String.valueOf(r6)
            java.lang.StringBuilder r2 = r2.append(r6)
            java.lang.String r0 = r2.toString()
            int r1 = r1 + 1
            goto L77
        L9f:
            return r0
        La0:
            if (r3 == 0) goto Lc0
            r3.close()     // Catch: java.io.IOException -> La6
            goto Lc0
        La6:
            r7 = move-exception
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.StringBuilder r0 = r8.append(r0)
            java.lang.String r8 = r7.getMessage()
            java.lang.StringBuilder r0 = r0.append(r8)
            java.lang.String r0 = r0.toString()
            android.util.Slog.e(r1, r0)
            r2 = 0
        Lc0:
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiMiPerfStubImpl.readFile(java.lang.String):java.lang.String");
    }

    public String getSystemBoostMode(String packageName, String activityName) {
        this.isCommon = false;
        this.isdefault = false;
        if (mMiperfXmlMap.containsKey(packageName)) {
            Object act_map = mMiperfXmlMap.get(packageName);
            HashMap<String, String> map_tmp = (HashMap) act_map;
            if (map_tmp.containsKey(activityName)) {
                if (this.isDebug) {
                    Slog.d(TAG, "Match BoostMode successfully, BoostMode is " + map_tmp.get(activityName));
                }
                return map_tmp.get(activityName);
            }
            if (map_tmp.containsKey("Common")) {
                this.isCommon = true;
                return map_tmp.get("Common");
            }
        } else {
            String thermalSconfig = readFile(THERMAL_SCONFIG);
            if (mThermalSconfigMap.containsKey(thermalSconfig) && mMiperfXmlMap.containsKey(DEFAULT_COMMON_PACKAGENAME)) {
                this.isdefault = true;
                this.isCommon = true;
                Object act_map2 = mMiperfXmlMap.get(DEFAULT_COMMON_PACKAGENAME);
                return ((HashMap) act_map2).get("Common");
            }
        }
        if (this.isDebug) {
            Slog.d(TAG, "Match BoostMode failed, there is no MiPerfBoost!");
            return "BOOSTMODE_NULL";
        }
        return "BOOSTMODE_NULL";
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0061, code lost:
    
        if (r1.equals("BOOSTMODE_NULL") != false) goto L39;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void miPerfSystemBoostNotify(int r6, java.lang.String r7, java.lang.String r8, java.lang.String r9) {
        /*
            r5 = this;
            r0 = 0
            java.lang.String r1 = r5.getSystemBoostMode(r7, r8)
            boolean r2 = r5.isCommon
            if (r2 == 0) goto Lb
            java.lang.String r8 = "Common"
        Lb:
            boolean r2 = r5.isdefault
            if (r2 == 0) goto L11
            java.lang.String r7 = "default.common.packagename"
        L11:
            boolean r2 = r5.isRecoverWrite
            r3 = 0
            if (r2 == 0) goto L25
            java.lang.String r2 = r5.lastPackName
            if (r7 != r2) goto L1e
            java.lang.String r2 = r5.lastActName
            if (r8 == r2) goto L25
        L1e:
            java.lang.String r2 = "WRITENODE_RECOVER"
            android.os.MiPerf.miPerfSystemBoostAcquire(r6, r7, r8, r2)
            r5.isRecoverWrite = r3
        L25:
            boolean r2 = r5.isRelease
            if (r2 == 0) goto L30
            java.lang.String r2 = "PERFLOCK_RELEASE"
            android.os.MiPerf.miPerfSystemBoostAcquire(r6, r7, r8, r2)
            r5.isRelease = r3
        L30:
            java.lang.String r2 = r5.lastPackName
            if (r7 != r2) goto L3a
            java.lang.String r2 = r5.lastActName
            if (r8 != r2) goto L3a
            java.lang.String r1 = "BOOSTMODE_NULL"
        L3a:
            r5.lastPackName = r7
            r5.lastActName = r8
            int r2 = r1.hashCode()
            r4 = 1
            switch(r2) {
                case -1721155661: goto L64;
                case -754587008: goto L5b;
                case 1186582712: goto L51;
                case 1394821899: goto L47;
                default: goto L46;
            }
        L46:
            goto L6e
        L47:
            java.lang.String r2 = "PERFLOCK_ACQUIRE"
            boolean r2 = r1.equals(r2)
            if (r2 == 0) goto L46
            r3 = r4
            goto L6f
        L51:
            java.lang.String r2 = "WRITENODE_ACQUIRE"
            boolean r2 = r1.equals(r2)
            if (r2 == 0) goto L46
            r3 = 2
            goto L6f
        L5b:
            java.lang.String r2 = "BOOSTMODE_NULL"
            boolean r2 = r1.equals(r2)
            if (r2 == 0) goto L46
            goto L6f
        L64:
            java.lang.String r2 = "MULTIPLEMODE"
            boolean r2 = r1.equals(r2)
            if (r2 == 0) goto L46
            r3 = 3
            goto L6f
        L6e:
            r3 = -1
        L6f:
            java.lang.String r2 = "MiuiMiPerfStubImpl"
            switch(r3) {
                case 0: goto L7e;
                case 1: goto L7b;
                case 2: goto L78;
                case 3: goto L75;
                default: goto L74;
            }
        L74:
            goto L88
        L75:
            r5.isRecoverWrite = r4
            goto L88
        L78:
            r5.isRecoverWrite = r4
            goto L88
        L7b:
            r5.isRelease = r4
            goto L88
        L7e:
            boolean r3 = r5.isDebug
            if (r3 == 0) goto L87
            java.lang.String r3 = "miPerfSystemBoostNotify: no MiPerfBoost!"
            android.util.Slog.d(r2, r3)
        L87:
            return
        L88:
            int r0 = android.os.MiPerf.miPerfSystemBoostAcquire(r6, r7, r8, r1)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "miPerfSystemBoostNotify: return = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r4 = ", System BoostScenes = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r9)
            java.lang.String r4 = ", BoostMode = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.String r4 = ", pid = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r6)
            java.lang.String r4 = ", packagename = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r7)
            java.lang.String r4 = ", activityname = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r8)
            java.lang.String r3 = r3.toString()
            android.util.Slog.d(r2, r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiMiPerfStubImpl.miPerfSystemBoostNotify(int, java.lang.String, java.lang.String, java.lang.String):void");
    }

    public void onAfterActivityResumed(ActivityRecord resumedActivity) {
        if (resumedActivity.app == null || resumedActivity.info == null) {
            return;
        }
        final int pid = resumedActivity.app.getPid();
        final String activityName = resumedActivity.info.name;
        final String packageName = resumedActivity.info.packageName;
        this.timerTask = new TimerTask() { // from class: com.android.server.wm.MiuiMiPerfStubImpl.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                try {
                    long startMiperfTime = System.currentTimeMillis();
                    MiuiMiPerfStubImpl.this.miPerfSystemBoostNotify(pid, packageName, activityName, "onAfterActivityResumed");
                    long durationMiperf = System.currentTimeMillis() - startMiperfTime;
                    if (durationMiperf > 50) {
                        Slog.w(MiuiMiPerfStubImpl.TAG, "Call miPerfSystemBoostNotify is timeout, took " + durationMiperf + "ms.");
                    }
                } catch (Exception e) {
                    Slog.e(MiuiMiPerfStubImpl.TAG, "miPerfSystemBoostNotify, IOException e:" + e.getMessage());
                }
            }
        };
        Timer timer = new Timer();
        this.timer = timer;
        timer.schedule(this.timerTask, 100L);
    }
}
