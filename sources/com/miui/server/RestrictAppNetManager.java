package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.server.net.NetworkManagementServiceStub;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class RestrictAppNetManager {
    private static final int RULE_ALLOW = 0;
    private static final int RULE_RESTRICT = 1;
    private static final String TAG = "RestrictAppNetManager";
    private static final int TYPE_ALL = 3;
    private static ArrayList<String> sRestrictedAppListBeforeRelease;
    private static NetworkManagementServiceStub sService;
    private static final Uri URI_CLOUD_DEVICE_RELEASED_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/device_released");
    private static long sLastUpdateTime = 0;
    private static final BroadcastReceiver mAppInstallReceiver = new BroadcastReceiver() { // from class: com.miui.server.RestrictAppNetManager.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            String packageName = uri != null ? uri.getSchemeSpecificPart() : null;
            int uid = intent.getIntExtra("android.intent.extra.UID", 0);
            boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            if (RestrictAppNetManager.sService != null && !TextUtils.isEmpty(packageName) && uid > 0 && !replacing && !RestrictAppNetManager.isAllowAccessInternet(packageName)) {
                RestrictAppNetManager.tryDownloadCloudData(context);
                Log.i(RestrictAppNetManager.TAG, "RULE_RESTRICT packageName: " + packageName);
                RestrictAppNetManager.sService.setMiuiFirewallRule(packageName, uid, 1, 3);
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void init(Context context) {
        boolean hasReleased = SystemProperties.getBoolean("persist.sys.released", false);
        Log.i(TAG, "init released : " + hasReleased);
        if (hasReleased) {
            return;
        }
        sService = NetworkManagementServiceStub.getInstance();
        registerCloudDataObserver(context);
        registerCloudDataObserver1(context);
        registerAppInstallReceiver(context);
        ArrayList<String> arrayList = new ArrayList<>();
        sRestrictedAppListBeforeRelease = arrayList;
        arrayList.add("com.antutu.ABenchMark");
        sRestrictedAppListBeforeRelease.add("com.antutu.ABenchMark5");
        sRestrictedAppListBeforeRelease.add("com.antutu.benchmark.bench64");
        sRestrictedAppListBeforeRelease.add("com.antutu.videobench");
        sRestrictedAppListBeforeRelease.add("com.antutu.ABenchMark.GL2");
        sRestrictedAppListBeforeRelease.add("com.antutu.tester");
        sRestrictedAppListBeforeRelease.add("com.antutu.benchmark.full");
        sRestrictedAppListBeforeRelease.add("com.music.videogame");
        sRestrictedAppListBeforeRelease.add("com.ludashi.benchmark");
        sRestrictedAppListBeforeRelease.add("com.ludashi.benchmarkhd");
        sRestrictedAppListBeforeRelease.add("com.qihoo360.ludashi.cooling");
        sRestrictedAppListBeforeRelease.add("cn.opda.android.activity");
        sRestrictedAppListBeforeRelease.add("com.shouji.cesupaofen");
        sRestrictedAppListBeforeRelease.add("com.colola.mobiletest");
        sRestrictedAppListBeforeRelease.add("ws.j7uxli.a6urcd");
        sRestrictedAppListBeforeRelease.add("com.gamebench.metricscollector");
        sRestrictedAppListBeforeRelease.add("com.huahua.test");
        sRestrictedAppListBeforeRelease.add("com.futuremark.dmandroid.application");
        sRestrictedAppListBeforeRelease.add("com.eembc.coremark");
        sRestrictedAppListBeforeRelease.add("com.rightware.BasemarkOSII");
        sRestrictedAppListBeforeRelease.add("com.glbenchmark.glbenchmark27");
        sRestrictedAppListBeforeRelease.add("com.greenecomputing.linpack");
        sRestrictedAppListBeforeRelease.add("eu.chainfire.cfbench");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench3");
        sRestrictedAppListBeforeRelease.add("com.quicinc.vellamo");
        sRestrictedAppListBeforeRelease.add("com.aurorasoftworks.quadrant.ui.advanced");
        sRestrictedAppListBeforeRelease.add("com.aurorasoftworks.quadrant.ui.standard");
        sRestrictedAppListBeforeRelease.add("eu.chainfire.perfmon");
        sRestrictedAppListBeforeRelease.add("com.evozi.deviceid");
        sRestrictedAppListBeforeRelease.add("com.finalwire.aida64");
        sRestrictedAppListBeforeRelease.add("com.cpuid.cpu_z");
        sRestrictedAppListBeforeRelease.add("rs.in.luka.android.pi");
        sRestrictedAppListBeforeRelease.add("com.uzywpq.cqlzahm");
        sRestrictedAppListBeforeRelease.add("com.xidige.androidinfo");
        sRestrictedAppListBeforeRelease.add("com.appems.hawkeye");
        sRestrictedAppListBeforeRelease.add("com.tyyj89.androidsuperinfo");
        sRestrictedAppListBeforeRelease.add("com.ft1gp");
        sRestrictedAppListBeforeRelease.add("ws.k6t2we.b4zyjdjv");
        sRestrictedAppListBeforeRelease.add("com.myapp.dongxie_app1");
        sRestrictedAppListBeforeRelease.add("com.shoujijiance.zj");
        sRestrictedAppListBeforeRelease.add("com.qrj.test");
        sRestrictedAppListBeforeRelease.add("com.appems.testonetest");
        sRestrictedAppListBeforeRelease.add("com.andromeda.androbench2");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench5.corporate");
        sRestrictedAppListBeforeRelease.add("net.kishonti.gfxbench.vulkan.v50000.corporate");
        sRestrictedAppListBeforeRelease.add("com.antutu.ABenchMark.lite");
        sRestrictedAppListBeforeRelease.add("com.antutu.aibenchmark");
        sRestrictedAppListBeforeRelease.add("com.ludashi.benchmark2");
        sRestrictedAppListBeforeRelease.add("com.ludashi.aibench");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench5c");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench5");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench4.corporate");
        sRestrictedAppListBeforeRelease.add("net.kishonti.gfxbench.gl.v40001.corporate");
        sRestrictedAppListBeforeRelease.add("org.benchmark.demo");
        sRestrictedAppListBeforeRelease.add("com.android.gputest");
        sRestrictedAppListBeforeRelease.add("android.test.app");
        sRestrictedAppListBeforeRelease.add("com.ioncannon.cpuburn.gpugflops");
        sRestrictedAppListBeforeRelease.add("ioncannon.com.andspecmod");
        sRestrictedAppListBeforeRelease.add("skynet.cputhrottlingtest");
        sRestrictedAppListBeforeRelease.add("com.primatelabs.geekbench6");
        updateFirewallRule(context, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isAllowAccessInternet(String packageName) {
        ArrayList<String> arrayList;
        boolean hasReleased = SystemProperties.getBoolean("persist.sys.released", false);
        if (hasReleased || (arrayList = sRestrictedAppListBeforeRelease) == null) {
            return true;
        }
        return true ^ arrayList.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateFirewallRule(Context context, int rule) {
        if (sRestrictedAppListBeforeRelease != null && sService != null) {
            Log.i(TAG, "updateFirewallRule : " + rule);
            Iterator<String> it = sRestrictedAppListBeforeRelease.iterator();
            while (it.hasNext()) {
                String pkgName = it.next();
                int uid = getUidByPackageName(context, pkgName);
                if (uid >= 0) {
                    sService.setMiuiFirewallRule(pkgName, uid, rule, 3);
                }
            }
        }
    }

    private static void registerCloudDataObserver(final Context context) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.miui.server.RestrictAppNetManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                RestrictAppNetManager.updateRestrictAppNetProp(context);
            }
        });
    }

    private static void registerCloudDataObserver1(final Context context) {
        context.getContentResolver().registerContentObserver(URI_CLOUD_DEVICE_RELEASED_NOTIFY, true, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.miui.server.RestrictAppNetManager.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Log.i(RestrictAppNetManager.TAG, "registerCloudDataObserver1");
                RestrictAppNetManager.updateFirewallRule(context, 0);
            }
        });
    }

    private static void registerAppInstallReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        context.registerReceiver(mAppInstallReceiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateRestrictAppNetProp(Context context) {
        try {
            boolean released = SystemProperties.getBoolean("persist.sys.released", false);
            if (!released) {
                Log.i(TAG, "updateRestrictAppNetProp");
                String deviceMode = Build.DEVICE;
                List<MiuiSettings.SettingsCloudData.CloudData> dataList = MiuiSettings.SettingsCloudData.getCloudDataList(context.getContentResolver(), "RestrictAppControl");
                if (dataList != null && dataList.size() != 0) {
                    for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                        if ("released".equals(data.getString(deviceMode, (String) null))) {
                            SystemProperties.set("persist.sys.released", "true");
                            updateFirewallRule(context, 0);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "update released prop exception", e);
        }
    }

    private static int getUidByPackageName(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
            return appInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "not find packageName :" + pkgName);
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void tryDownloadCloudData(Context context) {
        if (System.currentTimeMillis() - sLastUpdateTime > 86400000) {
            sLastUpdateTime = System.currentTimeMillis();
            Intent intent = new Intent("com.miui.action.UPDATE_RESTRICT_APP_DATA");
            intent.setFlags(BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT);
            context.sendBroadcastAsUser(intent, UserHandle.OWNER, "com.miui.permission.UPDATE_RESTRICT_DATA");
            Log.w(TAG, "send： com.miui.action.UPDATE_RESTRICT_APP_DATA");
        }
    }
}
