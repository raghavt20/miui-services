package com.miui.server.smartpower;

import android.os.RemoteException;
import com.miui.server.AccessController;
import com.miui.server.smartpower.SmartScenarioManager;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import miui.smartpower.IScenarioCallback;

/* loaded from: classes.dex */
public class SmartThermalPolicyManager implements SmartScenarioManager.ISmartScenarioCallback {
    private IScenarioCallback mCallBack;
    private SmartScenarioManager.ClientConfig mConfig;
    private SmartScenarioManager mSmartScenarioManager;

    public SmartThermalPolicyManager(SmartScenarioManager smartScenarioManager) {
        this.mSmartScenarioManager = smartScenarioManager;
    }

    private void init() {
        SmartScenarioManager.ClientConfig createClientConfig = this.mSmartScenarioManager.createClientConfig("thermal", this);
        this.mConfig = createClientConfig;
        createClientConfig.addMainScenarioIdConfig(2L, "com.tencent.mm", 2);
        this.mConfig.addMainScenarioIdConfig(4L, "com.sina.weibo", 2);
        this.mConfig.addMainScenarioIdConfig(8L, "com.ss.android.ugc.aweme", 2);
        this.mConfig.addMainScenarioIdConfig(16L, "com.smile.gifmaker", 2);
        this.mConfig.addMainScenarioIdConfig(32L, "com.ss.android.article.news", 2);
        this.mConfig.addMainScenarioIdConfig(64L, "com.taobao.taobao", 2);
        this.mConfig.addMainScenarioIdConfig(128L, "tv.danmaku.bili", 2);
        this.mConfig.addMainScenarioIdConfig(256L, "com.autonavi.minimap", 2);
        this.mConfig.addMainScenarioIdConfig(512L, AccessController.PACKAGE_CAMERA, 2);
        this.mConfig.addMainScenarioIdConfig(FormatBytesUtil.MB, "com.tencent.tmgp.sgame", 2);
        this.mConfig.addMainScenarioIdConfig(2097152L, "com.miHoYo.Yuanshen", 2);
        this.mConfig.addAdditionalScenarioIdConfig(2L, null, 64);
        this.mConfig.addAdditionalScenarioIdConfig(4L, "com.tencent.mm", 64);
        this.mConfig.addAdditionalScenarioIdConfig(8L, "com.tencent.mobileqq", 64);
        this.mConfig.addAdditionalScenarioIdConfig(16L, "com.ss.android.lark.kami", 64);
        this.mConfig.addAdditionalScenarioIdConfig(32L, "com.alibaba.android.rimet", 64);
        this.mConfig.addAdditionalScenarioIdConfig(64L, "com.tencent.wework", 64);
        this.mConfig.addAdditionalScenarioIdConfig(128L, "com.whatsapp", 64);
        this.mConfig.addAdditionalScenarioIdConfig(256L, "com.ss.android.lark", 64);
        this.mConfig.addAdditionalScenarioIdConfig(2048L, null, 128);
        this.mConfig.addAdditionalScenarioIdConfig(4096L, "com.tencent.mm", 128);
        this.mConfig.addAdditionalScenarioIdConfig(8192L, "com.tencent.mobileqq", 128);
        this.mConfig.addAdditionalScenarioIdConfig(16384L, "com.ss.android.lark.kami", 128);
        this.mConfig.addAdditionalScenarioIdConfig(32768L, "com.alibaba.android.rimet", 128);
        this.mConfig.addAdditionalScenarioIdConfig(65536L, "com.tencent.wework", 128);
        this.mConfig.addAdditionalScenarioIdConfig(131072L, "com.whatsapp", 128);
        this.mConfig.addAdditionalScenarioIdConfig(262144L, "com.ss.android.lark", 128);
        this.mConfig.addAdditionalScenarioIdConfig(2097152L, null, 256);
        this.mConfig.addAdditionalScenarioIdConfig(4194304L, "com.xiaomi.market", 256);
        this.mConfig.addAdditionalScenarioIdConfig(8388608L, "com.xunlei.downloadprovider", 256);
        this.mConfig.addAdditionalScenarioIdConfig(2147483648L, null, 16);
        this.mConfig.addAdditionalScenarioIdConfig(68719476736L, null, 32);
        this.mConfig.addAdditionalScenarioIdConfig(2199023255552L, "com.ss.android.lark.kami", 8);
        this.mConfig.addAdditionalScenarioIdConfig(4398046511104L, "com.alibaba.android.rimet", 8);
        this.mConfig.addAdditionalScenarioIdConfig(140737488355328L, null, 65536);
        this.mConfig.addAdditionalScenarioIdConfig(281474976710656L, null, 32768);
        this.mConfig.addAdditionalScenarioIdConfig(562949953421312L, null, 8192);
        this.mConfig.addAdditionalScenarioIdConfig(1125899906842624L, null, 2048);
        this.mConfig.addAdditionalScenarioIdConfig(2251799813685248L, null, 512);
        this.mConfig.addAdditionalScenarioIdConfig(72057594037927936L, null, 1024);
        this.mConfig.addAdditionalScenarioIdConfig(2305843009213693952L, null, 4096);
        this.mSmartScenarioManager.registClientConfig(this.mConfig);
    }

    public void registThermalScenarioCallback(IScenarioCallback callback) {
        this.mCallBack = callback;
        init();
    }

    @Override // com.miui.server.smartpower.SmartScenarioManager.ISmartScenarioCallback
    public void onCurrentScenarioChanged(long mainSenarioId, long additionalSenarioId) {
        IScenarioCallback iScenarioCallback = this.mCallBack;
        if (iScenarioCallback != null) {
            try {
                iScenarioCallback.onCurrentScenarioChanged(mainSenarioId, additionalSenarioId);
            } catch (RemoteException e) {
            }
        }
    }
}
