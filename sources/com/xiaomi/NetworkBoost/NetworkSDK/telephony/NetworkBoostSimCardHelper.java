package com.xiaomi.NetworkBoost.NetworkSDK.telephony;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.DualSimInfoManager;
import java.util.List;
import java.util.Map;
import miui.telephony.TelephonyManager;

/* loaded from: classes.dex */
public class NetworkBoostSimCardHelper {
    public static final String DEFAULT_NULL_IMSI = "default";
    private static final String TAG = "NetworkBoostSimCardHelper";
    private static NetworkBoostSimCardHelper sInstance;
    protected Context mContext;
    protected String mImsi1;
    private String mImsi2;
    protected boolean mIsSim1Inserted;
    private boolean mIsSim2Inserted;
    private DualSimInfoManager.ISimInfoChangeListener mSimInfoChangeListener;

    public static void init(Context context) {
        getInstance(context);
    }

    public static synchronized NetworkBoostSimCardHelper getInstance(Context context) {
        NetworkBoostSimCardHelper networkBoostSimCardHelper;
        synchronized (NetworkBoostSimCardHelper.class) {
            if (sInstance == null) {
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    sInstance = new NetworkBoostSimCardHelper(context);
                } else {
                    sInstance = new NetworkBoostSingleSimCardHelper(context);
                }
                sInstance.initForUIProcess();
            }
            networkBoostSimCardHelper = sInstance;
        }
        return networkBoostSimCardHelper;
    }

    private void initForUIProcess() {
    }

    private NetworkBoostSimCardHelper(Context context) {
        this.mImsi1 = DEFAULT_NULL_IMSI;
        this.mImsi2 = DEFAULT_NULL_IMSI;
        this.mSimInfoChangeListener = new DualSimInfoManager.ISimInfoChangeListener() { // from class: com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper.1
            public void onSubscriptionsChanged() {
                NetworkBoostSimCardHelper.this.updateSimState();
            }
        };
        this.mContext = context.getApplicationContext();
        updateSimState();
    }

    public boolean updateSimState() {
        List<Map<String, String>> siminfoList = null;
        try {
            siminfoList = DualSimInfoManager.getSimInfoList(this.mContext);
        } catch (Exception e) {
            Log.i(TAG, "get sim info exception!", e);
        }
        if (siminfoList == null || siminfoList.isEmpty()) {
            this.mIsSim1Inserted = false;
            this.mIsSim2Inserted = false;
            this.mImsi1 = DEFAULT_NULL_IMSI;
            this.mImsi2 = DEFAULT_NULL_IMSI;
            return true;
        }
        if (siminfoList.size() == 1) {
            Log.i(TAG, "one sim card inserted");
            Map<String, String> simInfo = siminfoList.get(0);
            int slotNum = Integer.parseInt(simInfo.get(Sim.SLOT_NUM));
            if (slotNum == 0) {
                this.mIsSim1Inserted = true;
                this.mIsSim2Inserted = false;
                this.mImsi1 = TelephonyUtil.getSubscriberId(this.mContext, slotNum);
                this.mImsi2 = DEFAULT_NULL_IMSI;
            } else if (slotNum == 1) {
                this.mIsSim1Inserted = false;
                this.mIsSim2Inserted = true;
                this.mImsi1 = DEFAULT_NULL_IMSI;
                this.mImsi2 = TelephonyUtil.getSubscriberId(this.mContext, slotNum);
            }
        } else if (siminfoList.size() == 2) {
            Log.i(TAG, "two sim cards inserted");
            this.mIsSim1Inserted = true;
            Map<String, String> simInfo2 = siminfoList.get(0);
            int slotNum2 = Integer.parseInt(simInfo2.get(Sim.SLOT_NUM));
            this.mImsi1 = TelephonyUtil.getSubscriberId(this.mContext, slotNum2);
            this.mIsSim2Inserted = true;
            Map<String, String> simInfo3 = siminfoList.get(1);
            int slotNum3 = Integer.parseInt(simInfo3.get(Sim.SLOT_NUM));
            this.mImsi2 = TelephonyUtil.getSubscriberId(this.mContext, slotNum3);
        } else {
            Log.i(TAG, "no sim card inserted");
        }
        return !isImsiMissed();
    }

    protected boolean isImsiMissed() {
        return (this.mIsSim1Inserted && this.mImsi1 == null) || (this.mIsSim2Inserted && this.mImsi2 == null);
    }

    public int getCurrentMobileSlotNum() {
        return TelephonyUtil.getCurrentMobileSlotNum();
    }

    public String getSimImsi(int slotNum) {
        if (slotNum == 0) {
            return this.mImsi1;
        }
        if (slotNum == 1) {
            return this.mImsi2;
        }
        return null;
    }

    /* loaded from: classes.dex */
    public static class NetworkBoostSingleSimCardHelper extends NetworkBoostSimCardHelper {
        public NetworkBoostSingleSimCardHelper(Context context) {
            super(context);
        }

        @Override // com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper
        public boolean updateSimState() {
            String newImsi = TelephonyUtil.getSubscriberId(this.mContext);
            if (TextUtils.isEmpty(newImsi)) {
                this.mIsSim1Inserted = false;
                newImsi = NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI;
            } else {
                this.mIsSim1Inserted = true;
            }
            if (!TextUtils.equals(newImsi, this.mImsi1)) {
                this.mImsi1 = newImsi;
            }
            return !isImsiMissed();
        }
    }
}
