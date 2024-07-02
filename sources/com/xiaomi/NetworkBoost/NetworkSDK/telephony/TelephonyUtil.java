package com.xiaomi.NetworkBoost.NetworkSDK.telephony;

import android.content.Context;
import android.util.Log;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManager;

/* loaded from: classes.dex */
public class TelephonyUtil {
    private static final String TAG = "TelephonyUtil";

    public static int getCurrentMobileSlotNum() {
        int currMobileSimId = SubscriptionManager.getDefault().getDefaultDataSlotId();
        if (currMobileSimId < 0 || currMobileSimId > 1) {
            return 0;
        }
        return currMobileSimId;
    }

    public static String getSubscriberId(Context context) {
        return getSubscriberId(context, 0);
    }

    public static String getSubscriberId(Context context, int slotNum) {
        Log.i(TAG, "getSubscriberId， slot: " + slotNum);
        return getImsi(context, slotNum);
    }

    private static String getImsi(Context context, int slotNum) {
        return TelephonyManager.getDefault().getSubscriberIdForSlot(slotNum);
    }
}
