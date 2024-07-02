package com.miui.app.smartpower;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class SmartPowerPolicyConstants {
    public static final long NEVER_PERIODIC_ACTIVE = 0;
    public static final int PROCESS_ACTIVE_LEVEL_CRITICAL = 1;
    public static final int PROCESS_ACTIVE_LEVEL_HEAVY = 0;
    public static final int PROCESS_ACTIVE_LEVEL_LOW = 3;
    public static final int PROCESS_ACTIVE_LEVEL_MODERATE = 2;
    public static final int PROCESS_USAGE_ACTIVE_LEVEL_DEFAULT = 9;
    public static final int PROCESS_USAGE_LEVEL_AUTOSTART = 4;
    public static final int PROCESS_USAGE_LEVEL_CRITICAL = 5;
    public static final int PROCESS_USAGE_LEVEL_LOW = 7;
    public static final int PROCESS_USAGE_LEVEL_MODERATE = 6;
    private static final String PROP_MIUI_OPTIMIZATION = "persist.sys.miui_optimization";
    public static boolean TESTSUITSPECIFIC = false;
    public static final int WHITE_LIST_ACTION_HIBERNATION = 2;
    public static final int WHITE_LIST_ACTION_INTERCEPT_ALARM = 8;
    public static final int WHITE_LIST_ACTION_INTERCEPT_PROVIDER = 32;
    public static final int WHITE_LIST_ACTION_INTERCEPT_SERVICE = 16;
    public static final int WHITE_LIST_ACTION_SCREENON_HIBERNATION = 4;
    public static final int WHITE_LIST_TYPE_ALARM_CLOUDCONTROL = 65536;
    public static final int WHITE_LIST_TYPE_ALARM_DEFAULT = 32768;
    public static final int WHITE_LIST_TYPE_ALARM_MASK = 114688;
    public static final int WHITE_LIST_TYPE_ALARM_MAX = 131072;
    public static final int WHITE_LIST_TYPE_ALARM_MIN = 16384;
    public static final int WHITE_LIST_TYPE_BACKUP = 64;
    public static final int WHITE_LIST_TYPE_CLOUDCONTROL = 4;
    public static final int WHITE_LIST_TYPE_CTS = 1024;
    public static final int WHITE_LIST_TYPE_DEFAULT = 2;
    public static final int WHITE_LIST_TYPE_DEPEND = 256;
    public static final int WHITE_LIST_TYPE_EXTAUDIO = 512;
    public static final int WHITE_LIST_TYPE_FREQUENTTHAW = 32;
    public static final int WHITE_LIST_TYPE_HIBERNATION_MASK = 2046;
    public static final int WHITE_LIST_TYPE_MAX = 2048;
    private static final int WHITE_LIST_TYPE_MIN = 1;
    public static final int WHITE_LIST_TYPE_PROVIDER_CLOUDCONTROL = 4194304;
    public static final int WHITE_LIST_TYPE_PROVIDER_DEFAULT = 2097152;
    public static final int WHITE_LIST_TYPE_PROVIDER_MASK = 7340032;
    public static final int WHITE_LIST_TYPE_PROVIDER_MAX = 8388608;
    public static final int WHITE_LIST_TYPE_PROVIDER_MIN = 1048576;
    public static final int WHITE_LIST_TYPE_SCREENON_CLOUDCONTROL = 8192;
    public static final int WHITE_LIST_TYPE_SCREENON_DEFAULT = 4096;
    public static final int WHITE_LIST_TYPE_SCREENON_MASK = 14336;
    public static final int WHITE_LIST_TYPE_SCREENON_MAX = 16384;
    private static final int WHITE_LIST_TYPE_SCREENON_MIN = 2048;
    public static final int WHITE_LIST_TYPE_SERVICE_CLOUDCONTROL = 524288;
    public static final int WHITE_LIST_TYPE_SERVICE_DEFAULT = 262144;
    public static final int WHITE_LIST_TYPE_SERVICE_MASK = 917504;
    public static final int WHITE_LIST_TYPE_SERVICE_MAX = 1048576;
    public static final int WHITE_LIST_TYPE_SERVICE_MIN = 131072;
    public static final int WHITE_LIST_TYPE_USB = 128;
    public static final int WHITE_LIST_TYPE_VPN = 8;
    public static final int WHITE_LIST_TYPE_WALLPAPER = 16;

    public static void updateTestSuitSpecificEnable() {
        TESTSUITSPECIFIC = !SystemProperties.getBoolean("persist.sys.miui_optimization", !"1".equals(SystemProperties.get("ro.miui.cts")));
    }
}
