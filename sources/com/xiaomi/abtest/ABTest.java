package com.xiaomi.abtest;

import android.content.Context;
import com.xiaomi.abtest.d.a;
import com.xiaomi.abtest.d.k;
import java.util.Map;

/* loaded from: classes.dex */
public class ABTest {
    private static final String a = "ABTest";
    private ABTestConfig b;

    /* loaded from: classes.dex */
    public interface OnLoadRemoteConfigListener {
        void onLoadCompleted();
    }

    private ABTest() {
    }

    public static ABTest abTestWithConfig(Context context, ABTestConfig config) {
        a.a(context);
        k.a();
        k.a(a, "abTestWithConfig start,config: " + (config == null ? "" : config.toString()));
        ABTest aBTest = new ABTest();
        aBTest.b = config;
        com.xiaomi.abtest.a.a.a(config);
        com.xiaomi.abtest.a.a.b().a(config.getAppName());
        return aBTest;
    }

    public static void setIsLoadConfigWhenBackground(boolean isLoad) {
        com.xiaomi.abtest.a.a.b().a(isLoad);
    }

    public Map<String, ExperimentInfo> getExperiments(Map<String, String> conditions) {
        com.xiaomi.abtest.a.a b = com.xiaomi.abtest.a.a.b();
        com.xiaomi.abtest.b.a aVar = new com.xiaomi.abtest.b.a();
        aVar.c(this.b.getAppName());
        aVar.b(this.b.getDeviceId());
        aVar.a(conditions);
        aVar.a(this.b.getUserId());
        return b.a(aVar);
    }

    public void loadRemoteConfig(OnLoadRemoteConfigListener listener) {
        com.xiaomi.abtest.a.a.b().a(listener);
    }

    public void clearSingleInstance() {
        com.xiaomi.abtest.a.a.b().a();
    }
}
