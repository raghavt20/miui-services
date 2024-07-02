package com.miui.server.greeze;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class GreezeManagerDebugConfig {
    public static boolean DEBUG = false;
    static boolean DEBUG_AIDL = false;
    public static boolean DEBUG_LAUNCH_FROM_HOME = false;
    static boolean DEBUG_MILLET = false;
    public static boolean DEBUG_SKIPUID = false;
    static final String PROPERTY_GZ_CGROUPV1 = "persist.sys.millet.cgroup";
    static final String PROPERTY_GZ_DEBUG = "persist.sys.gz.debug";
    static final String PROPERTY_GZ_HANDSHAKE = "persist.sys.millet.handshake";
    static final String PROPERTY_GZ_MONKEY = "persist.sys.gz.monkey";
    static final boolean DEBUG_MONKEY = SystemProperties.getBoolean(PROPERTY_GZ_MONKEY, false);
    static final String PROPERTY_GZ_FZTIMEOUT = "persist.sys.gz.fztimeout";
    public static final long LAUNCH_FZ_TIMEOUT = SystemProperties.getLong(PROPERTY_GZ_FZTIMEOUT, 5000);
    static final String PROPERTY_PID_DEBUG = "persist.sys.pid.debug";
    public static boolean PID_DEBUG = SystemProperties.getBoolean(PROPERTY_PID_DEBUG, false);
    static final String PROP_POWERMILLET_ENABLE = "persist.sys.powmillet.enable";
    public static final boolean mPowerMilletEnable = SystemProperties.getBoolean(PROP_POWERMILLET_ENABLE, false);
    public static boolean mCgroupV1Flag = false;
    private static final String PROPERTY_GZ_ENABLE = "persist.sys.gz.enable";
    protected static boolean sEnable = SystemProperties.getBoolean(PROPERTY_GZ_ENABLE, false);
    protected static boolean milletEnable = false;

    static {
        boolean z = SystemProperties.getBoolean(PROPERTY_GZ_DEBUG, false);
        DEBUG = z;
        DEBUG_SKIPUID = z;
        DEBUG_MILLET = z;
        DEBUG_AIDL = z;
        DEBUG_LAUNCH_FROM_HOME = z;
    }

    public static boolean isEnable() {
        return sEnable && milletEnable;
    }
}
