package com.android.server.am;

import android.os.Process;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.util.List;

/* loaded from: classes.dex */
public interface IProcessPolicy {
    public static final int FLAG_CLOUD_WHITE_LIST = 4;
    public static final int FLAG_DISABLE_FORCE_STOP = 32;
    public static final int FLAG_DISABLE_TRIM_MEMORY = 16;
    public static final int FLAG_DYNAMIC_WHITE_LIST = 2;
    public static final int FLAG_ENABLE_CALL_PROTECT = 64;
    public static final int FLAG_ENTERPRISE_APP_LIST = 4096;
    public static final int FLAG_FAST_BOOT_APP_LIST = 2048;
    public static final int FLAG_NEED_TRACE_LIST = 128;
    public static final int FLAG_SECRETLY_PROTECT_APP_LIST = 1024;
    public static final int FLAG_STATIC_WHILTE_LIST = 1;
    public static final int FLAG_USER_DEFINED_LIST = 8;
    public static final int FLAG_USER_REQUEST_CLEAN_WHITE_LIST = 8192;
    public static final String REASON_ANR = "anr";
    public static final String REASON_AUTO_IDLE_KILL = "AutoIdleKill";
    public static final String REASON_AUTO_LOCK_OFF_CLEAN = "AutoLockOffClean";
    public static final String REASON_AUTO_LOCK_OFF_CLEAN_BY_PRIORITY = "AutoLockOffCleanByPriority";
    public static final String REASON_AUTO_POWER_KILL = "AutoPowerKill";
    public static final String REASON_AUTO_SLEEP_CLEAN = "AutoSleepClean";
    public static final String REASON_AUTO_SYSTEM_ABNORMAL_CLEAN = "AutoSystemAbnormalClean";
    public static final String REASON_AUTO_THERMAL_KILL = "AutoThermalKill";
    public static final String REASON_AUTO_THERMAL_KILL_ALL_LEVEL_1 = "AutoThermalKillAll1";
    public static final String REASON_AUTO_THERMAL_KILL_ALL_LEVEL_2 = "AutoThermalKillAll2";
    public static final String REASON_CRASH = "crash";
    public static final String REASON_DISPLAY_SIZE_CHANGED = "DisplaySizeChanged";
    public static final String REASON_FORCE_CLEAN = "ForceClean";
    public static final String REASON_GAME_CLEAN = "GameClean";
    public static final String REASON_GARBAGE_CLEAN = "GarbageClean";
    public static final String REASON_LOCK_SCREEN_CLEAN = "LockScreenClean";
    public static final String REASON_LOW_MEMO = "lowMemory";
    public static final String REASON_MIUI_MEMO_SERVICE = "MiuiMemoryService";
    public static final String REASON_ONE_KEY_CLEAN = "OneKeyClean";
    public static final String REASON_OPTIMIZATION_CLEAN = "OptimizationClean";
    public static final String REASON_SCREEN_OFF_CPU_CHECK_KILL = "ScreenOffCPUCheckKill";
    public static final String REASON_SWIPE_UP_CLEAN = "SwipeUpClean";
    public static final String REASON_UNKNOWN = "Unknown";
    public static final String REASON_USER_DEFINED = "UserDefined";
    public static final String TAG_PM = "ProcessManager";
    public static final boolean TAG_WITH_CLASS_NAME = false;
    public static final int USER_ALL = -100;

    List<Integer> getActiveUidList(int i);

    boolean isLockedApplication(String str, int i);

    static Long getMemoryThresholdForFastBooApp() {
        long memoryThreshold;
        long deviceTotalMem = Process.getTotalMemory() / FormatBytesUtil.GB;
        if (deviceTotalMem < 4) {
            memoryThreshold = 819200;
        } else if (deviceTotalMem < 8) {
            memoryThreshold = 1536000;
        } else {
            memoryThreshold = 2048000;
        }
        return Long.valueOf(memoryThreshold);
    }

    static void setAppMaxProcState(ProcessRecord app, int targetMaxProcState) {
        app.mState.mMaxProcState = targetMaxProcState;
    }

    static int getAppMaxProcState(ProcessRecord app) {
        return app.mState.mMaxProcState;
    }
}
