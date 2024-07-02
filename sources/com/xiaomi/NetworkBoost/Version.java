package com.xiaomi.NetworkBoost;

/* loaded from: classes.dex */
public class Version {
    public static int a;

    public static int getServiceVersion() {
        return a;
    }

    public static boolean isSupport(int i) {
        return i <= a;
    }

    public static void setServiceVersion(int i) {
        a = i;
    }
}
