package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.xiaomi.abtest.ABTest;
import com.xiaomi.abtest.ABTestConfig;
import com.xiaomi.abtest.ExperimentInfo;
import java.util.Map;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ABTestHelper {
    private static final long DELAY_TIME = 5000;
    private static final String PATH = "/ExpLayer/ExpDomain/";
    private static final String TAG = "ABTestHelper";
    private static final int UPDATE_EXPERIMENTS = 0;
    private static final long UPDATE_EXPERIMENTS_DELAY = 600000;
    private ABTest mABTest;
    private String mAppName;
    private Context mContext;
    private Consumer<Void> mEndExperiment;
    private Map<String, String> mExpCondition;
    private int mExpId = -1;
    private Handler mHandler;
    private String mLayerName;
    private Consumer<Map<String, String>> mStartExperiment;
    private TelephonyManager mTelephonyManager;

    public ABTestHelper(Looper looper, Context context, String appName, String layerName, Map<String, String> expCondition, Consumer<Map<String, String>> startExperiment, Consumer<Void> endExperiment, long delay) {
        this.mHandler = new AbTestHandler(looper);
        this.mContext = context;
        this.mAppName = appName;
        this.mLayerName = layerName;
        this.mExpCondition = expCondition;
        this.mStartExperiment = startExperiment;
        this.mEndExperiment = endExperiment;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.ABTestHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ABTestHelper.this.lambda$new$0();
            }
        }, delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: init, reason: merged with bridge method [inline-methods] */
    public void lambda$new$0() {
        String imei = getImei();
        if (imei == null) {
            Slog.e(TAG, "failed to init, imei = null !!");
            return;
        }
        ABTestConfig.Builder builder = new ABTestConfig.Builder();
        ABTestConfig conf = builder.setAppName(this.mAppName).setLayerName(this.mLayerName).setDeviceId(imei).setUserId(imei).build();
        this.mABTest = ABTest.abTestWithConfig(this.mContext, conf);
        ABTest.setIsLoadConfigWhenBackground(true);
        this.mHandler.sendEmptyMessageDelayed(0, DELAY_TIME);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateABTest() {
        this.mHandler.removeMessages(0);
        Map<String, ExperimentInfo> experiments = this.mABTest.getExperiments(this.mExpCondition);
        Slog.i(TAG, "experiments: " + experiments);
        if (experiments != null && experiments.get("/" + this.mAppName + "/ExpLayer/ExpDomain/" + this.mLayerName) != null) {
            ExperimentInfo experimentInfo = experiments.get("/" + this.mAppName + "/ExpLayer/ExpDomain/" + this.mLayerName);
            Map<String, String> expParams = experimentInfo.getParams();
            int expId = experimentInfo.getExpId();
            this.mExpId = expId;
            if (expParams != null) {
                Slog.d(TAG, "start experiment mExpId: " + this.mExpId + ", mAppName: " + this.mAppName + ", mLayerName: " + this.mLayerName + ", expParams: " + expParams);
                this.mStartExperiment.accept(expParams);
            }
        }
        if (this.mExpId != -1 && (experiments == null || experiments.get("/" + this.mAppName + "/ExpLayer/ExpDomain/" + this.mLayerName) == null)) {
            Slog.d(TAG, "end experiment mExpId: " + this.mExpId);
            this.mExpId = -1;
            this.mEndExperiment.accept(null);
        }
        this.mHandler.sendEmptyMessageDelayed(0, 600000L);
    }

    public int getExpId() {
        return this.mExpId;
    }

    private String getImei() {
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager == null) {
            return null;
        }
        String imei = telephonyManager.getDeviceId();
        return imei;
    }

    /* loaded from: classes.dex */
    private final class AbTestHandler extends Handler {
        public AbTestHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    ABTestHelper.this.updateABTest();
                    return;
                default:
                    return;
            }
        }
    }
}
