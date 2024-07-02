package com.miui.app.smartpower;

import android.os.SystemProperties;
import com.android.server.content.SyncManagerStubImpl;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;

/* loaded from: classes.dex */
public class SmartPowerSettings {
    public static final int EVENT_TAGS = 90098;
    private static final String PROPERTY_PREFIX = "persist.sys.smartpower.";
    public static final String TIME_FORMAT_PATTERN = "HH:mm:ss.SSS";
    public static final String USB_DATA_TRANS_PROC = "android.process.media";
    public static final boolean DEBUG_ALL = SystemProperties.getBoolean("persist.sys.smartpower.debug", false);
    public static final boolean DEBUG_PROC = SystemProperties.getBoolean("persist.sys.smartpower.debug.proc", false);
    public static final boolean PROP_FROZEN_CGROUPV1_ENABLE = SystemProperties.getBoolean("persist.sys.millet.cgroup", false);
    public static final String APP_STATE_PROP = "persist.sys.smartpower.appstate.enable";
    public static boolean APP_STATE_ENABLE = SystemProperties.getBoolean(APP_STATE_PROP, true);
    public static final String FROZEN_PROP = "persist.sys.smartpower.fz.enable";
    public static boolean PROP_FROZEN_ENABLE = SystemProperties.getBoolean(FROZEN_PROP, false);
    public static final String INTERCEPT_PROP = "persist.sys.smartpower.intercept.enable";
    public static boolean PROP_INTERCEPT_ENABLE = SystemProperties.getBoolean(INTERCEPT_PROP, false);
    public static final String DISPLAY_POLICY_PROP = "persist.sys.smartpower.display.enable";
    public static boolean DISPLAY_POLICY_ENABLE = SystemProperties.getBoolean(DISPLAY_POLICY_PROP, false);
    public static final String WINDOW_POLICY_PROP = "persist.sys.smartpower.window.enable";
    public static boolean WINDOW_POLICY_ENABLE = SystemProperties.getBoolean(WINDOW_POLICY_PROP, true);
    public static final long MAX_HISTORY_REPORT_DURATION = SystemProperties.getLong("persist.sys.smartpower.history.dur", 14400000);
    public static final int MAX_HISTORY_REPORT_SIZE = SystemProperties.getInt("persist.sys.smartpower.history.size", 100);
    public static final String GAME_PAY_PROTECT = "persist.sys.smartpower.gamepay.protect.enabled";
    public static boolean GAME_PAY_PROTECT_ENABLED = SystemProperties.getBoolean(GAME_PAY_PROTECT, true);
    public static final long INACTIVE_DURATION = SystemProperties.getLong("persist.sys.smartpower.inactive.dur", 3000);
    public static final long INACTIVE_SPTM_DURATION = SystemProperties.getLong("persist.sys.smartpower.inactive.sptm.dur", 2000);
    public static final long HIBERNATE_DURATION = SystemProperties.getLong("persist.sys.smartpower.hibernate.dur", 600000);
    public static final long MAX_HIBERNATE_DURATION = SystemProperties.getLong("persist.sys.smartpower.hibernate.max.dur", SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION);
    public static final long IDLE_DURATION = SystemProperties.getLong("persist.sys.smartpower.idle.dur", 2000);
    public static final long MAINTENANCE_DURATION = SystemProperties.getLong("persist.sys.smartpower.maintenance.dur", 2000);
    public static final long UPDATE_USERSTATS_DURATION = SystemProperties.getLong("persist.sys.smartpower.update.userstats.dur", SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED);
    public static final long DEF_RES_NET_ACTIVE_SPEED = SystemProperties.getInt("persist.sys.smartpower.res.netactive.speed", 500);
    public static final long DEF_RES_NET_MONITOR_PERIOD = SystemProperties.getInt("persist.sys.smartpower.res.netmonitor.periodic", 1000);
    public static final long DEF_LAST_INTER_ACTIVE_DURATION = SystemProperties.getInt("persist.sys.smartpower.lastinteractive.dur", 5000);
    public static long PROC_MEM_LVL1_PSS_LIMIT_KB = SystemProperties.getLong("persist.sys.smartpower.memthreshold.app.lvl1", 614400);
    public static long PROC_MEM_LVL2_PSS_LIMIT_KB = SystemProperties.getLong("persist.sys.smartpower.memthreshold.app.lvl2", 819200);
    public static long PROC_MEM_LVL3_PSS_LIMIT_KB = SystemProperties.getLong("persist.sys.smartpower.memthreshold.app.lvl3", FormatBytesUtil.MB);
    public static final int PROP_LIMIT_MAX_REFRESH_RATE = SystemProperties.getInt("persist.sys.smartpower.limit.max.refresh.rate", -1);
    public static final boolean PROP_CAMERA_REFRESH_RATE_ENABLE = SystemProperties.getBoolean("persist.sys.smartpower.display_camera_fps_enable", false);
    public static final int PROP_THERMAL_TEMP_THRESHOLD = SystemProperties.getInt("persist.sys.smartpower.display_thermal_temp_threshold", 46);
}
