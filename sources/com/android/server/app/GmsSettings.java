package com.android.server.app;

import android.util.Slog;
import java.util.HashSet;

/* loaded from: classes.dex */
public class GmsSettings {
    private static final HashSet<String> mLocalDevicesForPowerSaving;

    static {
        HashSet<String> hashSet = new HashSet<>();
        mLocalDevicesForPowerSaving = hashSet;
        hashSet.add("aurora");
        hashSet.add("shennong");
        hashSet.add("houji");
        hashSet.add("chenfeng");
        hashSet.add("vermeer");
        hashSet.add("duchamp");
        hashSet.add("manet");
        hashSet.add("peridot");
        hashSet.add("gold");
        hashSet.add("sheng");
        hashSet.add("liuqin");
        hashSet.add("yudi");
        hashSet.add("ishtar");
        hashSet.add("marble");
        hashSet.add("corot");
        hashSet.add("babylon");
        hashSet.add("nuwa");
        hashSet.add("fuxi");
        hashSet.add("redwood");
        hashSet.add("yuechu");
        hashSet.add("pipa");
        hashSet.add("zeus");
        hashSet.add("cupid");
        hashSet.add("ingres");
        hashSet.add("unicorn");
        hashSet.add("mayfly");
        hashSet.add("thor");
        hashSet.add("ziyi");
        hashSet.add("rubens");
        hashSet.add("munch");
        hashSet.add("matisse");
        hashSet.add("venus");
        hashSet.add("odin");
        hashSet.add("star");
    }

    public static void getLocalDevicesForPowerSaving(HashSet<String> devices, HashSet<String> offLocalDevices) {
        if (devices != null) {
            Slog.d("GameManagerServiceStub", "cloud devices = " + devices);
            devices.addAll(mLocalDevicesForPowerSaving);
        }
        if (devices != null && offLocalDevices != null && !offLocalDevices.isEmpty()) {
            Slog.d("GameManagerServiceStub", "offLocalDevices = " + offLocalDevices);
            devices.removeAll(offLocalDevices);
        }
        if (devices != null) {
            Slog.d("GameManagerServiceStub", "final devices = " + devices);
        }
    }
}
