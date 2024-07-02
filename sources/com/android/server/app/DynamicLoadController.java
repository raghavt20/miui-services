package com.android.server.app;

import android.compat.Compatibility;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.server.compat.PlatformCompat;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.ActivityTaskManagerService;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
public class DynamicLoadController {
    private static final String TAG = "DynamicLoadController";
    private static HashMap<String, AppInfo> mApps;
    final ArraySet<Long> DOWNSCALE_CHANGE_IDS = new ArraySet<>(new Long[]{168419799L, 182811243L, 189969734L, 176926753L, 189969779L, 176926829L, 189969744L, 176926771L, 189970036L, 176926741L, 189969782L, 189970038L, 189969749L, 189970040L});
    ActivityTaskManagerService mActivityTaskManager;

    static {
        HashMap<String, AppInfo> hashMap = new HashMap<>();
        mApps = hashMap;
        hashMap.put("com.sina.weibo", new AppInfo("com.sina.weibo", "0.75"));
        mApps.put("com.ss.android.article.news", new AppInfo("com.ss.android.article.news", "0.75"));
        mApps.put("com.taobao.taobao", new AppInfo("com.taobao.taobao", "0.75"));
        mApps.put("com.kuaishou.nebula", new AppInfo("com.kuaishou.nebula", "0.75"));
        mApps.put("com.ss.android.ugc.aweme", new AppInfo("com.ss.android.ugc.aweme", "0.75"));
        mApps.put("com.baidu.BaiduMap", new AppInfo("com.baidu.BaiduMap", "0.75"));
        mApps.put(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, new AppInfo(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "0.75"));
        mApps.put("com.ss.android.ugc.aweme.lite", new AppInfo("com.ss.android.ugc.aweme.lite", "0.75"));
        mApps.put("com.gotokeep.keep", new AppInfo("com.gotokeep.keep", "0.75"));
        mApps.put("com.dianping.v1 ", new AppInfo("com.dianping.v1 ", "0.75"));
        mApps.put("com.UCMobile", new AppInfo("com.UCMobile", "0.75"));
        mApps.put("com.jingdong.app.mall", new AppInfo("com.jingdong.app.mall", "0.75"));
        mApps.put("com.alibaba.android.rimet", new AppInfo("com.alibaba.android.rimet", "0.75"));
        mApps.put("com.kugou.android", new AppInfo("com.kugou.android", "0.75"));
        mApps.put("com.tencent.qqmusic", new AppInfo("com.tencent.qqmusic", "0.75"));
        mApps.put("com.douban.frodo", new AppInfo("com.douban.frodo", "0.75"));
        mApps.put("com.wuba", new AppInfo("com.wuba", "0.75"));
        mApps.put("com.sinyee.babybus.recommendapp", new AppInfo("com.sinyee.babybus.recommendapp", "0.75"));
        mApps.put("com.jifen.qukan", new AppInfo("com.jifen.qukan", "0.75"));
        mApps.put("com.yuncheapp.android.pearl", new AppInfo("com.yuncheapp.android.pearl", "0.75"));
        mApps.put("com.duoduo.child.story", new AppInfo("com.duoduo.child.story", "0.75"));
        mApps.put("com.bokecc.dance", new AppInfo("com.bokecc.dance", "0.75"));
        mApps.put("com.guotai.dazhihui", new AppInfo("com.guotai.dazhihui", "0.75"));
        mApps.put("com.cubic.autohome", new AppInfo("com.cubic.autohome", "0.75"));
        mApps.put("com.able.wisdomtree", new AppInfo("com.able.wisdomtree", "0.75"));
        mApps.put("com.snda.wifilocating", new AppInfo("com.snda.wifilocating", "0.75"));
        mApps.put("com.hexin.plat.android", new AppInfo("com.hexin.plat.android", "0.75"));
        mApps.put("cn.xiaochuankeji.zuiyouLite ", new AppInfo("cn.xiaochuankeji.zuiyouLite", "0.75"));
        mApps.put("com.sinyee.babybus.recommendapp", new AppInfo("com.sinyee.babybus.recommendapp", "0.75"));
        mApps.put("com.mampod.ergedd", new AppInfo("com.mampod.ergedd", "0.75"));
    }

    public DynamicLoadController(ActivityTaskManagerService activityTaskManager) {
        this.mActivityTaskManager = activityTaskManager;
    }

    public boolean dynamicLoadWithPackageName(String ratio, String packageName) {
        return downscaleApp(ratio, packageName);
    }

    private boolean downscaleApp(String ratio, String packageName) {
        Set<Long> disabled;
        if (!mApps.containsKey(packageName)) {
            Slog.e(TAG, "invalid packageName, reject!!!");
            return false;
        }
        final long changeId = getCompatChangeId(ratio);
        Set<Long> enabled = new ArraySet<>();
        if (changeId == 0) {
            disabled = this.DOWNSCALE_CHANGE_IDS;
        } else {
            enabled.add(168419799L);
            enabled.add(Long.valueOf(changeId));
            disabled = (Set) this.DOWNSCALE_CHANGE_IDS.stream().filter(new Predicate() { // from class: com.android.server.app.DynamicLoadController$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DynamicLoadController.lambda$downscaleApp$0(changeId, (Long) obj);
                }
            }).collect(Collectors.toSet());
        }
        PlatformCompat platformCompat = ServiceManager.getService("platform_compat");
        CompatibilityChangeConfig overrides = new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
        platformCompat.setOverrides(overrides, packageName);
        Slog.v(TAG, "dynamic load enable packageName = " + packageName + " ratio = " + ratio);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$downscaleApp$0(long changeId, Long it) {
        return (it.longValue() == 168419799 || it.longValue() == changeId) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AppInfo {
        String packageName;
        String ratio;

        public AppInfo(String packageName, String ratio) {
            this.packageName = packageName;
            this.ratio = ratio;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static long getCompatChangeId(String raw) {
        char c;
        switch (raw.hashCode()) {
            case 47605:
                if (raw.equals("0.3")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 47606:
                if (raw.equals("0.4")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 47607:
                if (raw.equals("0.5")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 47608:
                if (raw.equals("0.6")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 47609:
                if (raw.equals("0.7")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 47610:
                if (raw.equals("0.8")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 47611:
                if (raw.equals("0.9")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1475808:
                if (raw.equals("0.35")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1475839:
                if (raw.equals("0.45")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1475870:
                if (raw.equals("0.55")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1475901:
                if (raw.equals("0.65")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1475932:
                if (raw.equals("0.75")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 1475963:
                if (raw.equals("0.85")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 189970040L;
            case 1:
                return 189969749L;
            case 2:
                return 189970038L;
            case 3:
                return 189969782L;
            case 4:
                return 176926741L;
            case 5:
                return 189970036L;
            case 6:
                return 176926771L;
            case 7:
                return 189969744L;
            case '\b':
                return 176926829L;
            case '\t':
                return 189969779L;
            case '\n':
                return 176926753L;
            case 11:
                return 189969734L;
            case '\f':
                return 182811243L;
            default:
                return 0L;
        }
    }
}
