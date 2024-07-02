package com.xiaomi.NetworkBoost.NetworkSDK.telephony;

import android.content.Context;
import java.util.List;
import java.util.Map;
import miui.securitycenter.DualSim.DualSimInfoManagerWrapper;

/* loaded from: classes.dex */
public class DualSimInfoManager {

    /* loaded from: classes.dex */
    public interface ISimInfoChangeListener extends DualSimInfoManagerWrapper.ISimInfoChangeWrapperListener {
    }

    public static void registerChangeListener(Context ctx, DualSimInfoManagerWrapper.ISimInfoChangeWrapperListener listener) {
    }

    public static void unRegisterChangeListener(Context ctx, DualSimInfoManagerWrapper.ISimInfoChangeWrapperListener listener) {
        DualSimInfoManagerWrapper.unRegisterSimInfoChangeListener(ctx, listener);
    }

    public static List<Map<String, String>> getSimInfoList(Context context) {
        return DualSimInfoManagerWrapper.getSimInfoList(context);
    }
}
