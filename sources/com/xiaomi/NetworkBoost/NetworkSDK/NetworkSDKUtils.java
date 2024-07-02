package com.xiaomi.NetworkBoost.NetworkSDK;

import android.net.wifi.WlanLinkLayerQoE;
import com.xiaomi.NetworkBoost.NetLinkLayerQoE;
import miui.android.animation.internal.AnimTask;

/* loaded from: classes.dex */
public class NetworkSDKUtils {
    public static synchronized NetLinkLayerQoE copyFromNetWlanLinkLayerQoE(WlanLinkLayerQoE source, NetLinkLayerQoE copyResult) {
        synchronized (NetworkSDKUtils.class) {
            if (copyResult == null) {
                copyResult = new NetLinkLayerQoE();
            }
            if (source == null) {
                return copyResult;
            }
            copyResult.setVersion(source.getVersion());
            copyResult.setSsid(source.getSsid());
            copyResult.setRssi_mgmt(source.getRssi_mgmt());
            copyResult.setMpduLostRatio(source.getMpduLostRatio());
            copyResult.setRetriesRatio(source.getRetriesRatio());
            copyResult.setFrequency(source.getFrequency());
            copyResult.setRadioOnTimeMs(source.getRadioOnTimeMs());
            copyResult.setCcaBusyTimeMs(source.getCcaBusyTimeMs());
            copyResult.setBw(source.getBw());
            copyResult.setRateMcsIdx(source.getRateMcsIdx());
            copyResult.setBitRateInKbps(source.getBitRateInKbps());
            copyResult.setRxmpdu_be(source.getRxmpdu_be());
            copyResult.setTxmpdu_be(source.getTxmpdu_be());
            copyResult.setLostmpdu_be(source.getLostmpdu_be());
            copyResult.setRetries_be(source.getRetries_be());
            copyResult.setRxmpdu_bk(source.getRxmpdu_bk());
            copyResult.setTxmpdu_bk(source.getTxmpdu_bk());
            copyResult.setLostmpdu_bk(source.getLostmpdu_bk());
            copyResult.setRetries_bk(source.getRetries_bk());
            copyResult.setRxmpdu_vi(source.getRxmpdu_vi());
            copyResult.setTxmpdu_vi(source.getTxmpdu_vi());
            copyResult.setLostmpdu_vi(source.getLostmpdu_vi());
            copyResult.setRetries_vi(source.getRetries_vi());
            copyResult.setRxmpdu_vo(source.getRxmpdu_vo());
            copyResult.setTxmpdu_vo(source.getTxmpdu_vo());
            copyResult.setLostmpdu_vo(source.getLostmpdu_vo());
            copyResult.setRetries_vo(source.getRetries_vo());
            return copyResult;
        }
    }

    public static int intervalTransformation(int interval) {
        switch (interval) {
            case 0:
                return 100;
            case 1:
                return 200;
            case 2:
                return 1000;
            case 3:
                return 2000;
            case 4:
                return AnimTask.MAX_SINGLE_TASK_SIZE;
            case 5:
                return 8000;
            default:
                return interval;
        }
    }
}
