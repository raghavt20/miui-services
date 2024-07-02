package com.android.server.powerconsumpiton;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import com.android.server.MiuiCommonCloudHelperStub;
import com.android.server.powerconsumpiton.couldEntity.ActivityDimInfo;
import com.android.server.powerconsumpiton.couldEntity.DeclineWhiteInfo;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/* loaded from: classes.dex */
public final class PowerConsumptionServiceCouldData {
    private static final String POWER_CONSUMPTION_CLOUD_FILE = "power_consumption_cloud_config.xml";
    private Context mContext;
    private MiuiCommonCloudHelperStub mMiuiCommonCloudHelper;
    private Handler mPCBgHandler;
    private final HashMap<String, int[]> mActivityPolicies = new HashMap<>();
    private final HashMap<String, int[]> mWindowPolicies = new HashMap<>();

    public PowerConsumptionServiceCouldData(Context context, Handler handler) {
        this.mContext = context;
        this.mPCBgHandler = handler;
        init();
    }

    private void init() {
        MiuiCommonCloudHelperStub miuiCommonCloudHelperStub = MiuiCommonCloudHelperStub.getInstance();
        this.mMiuiCommonCloudHelper = miuiCommonCloudHelperStub;
        miuiCommonCloudHelperStub.init(this.mContext, this.mPCBgHandler, Environment.getDownloadCacheDirectory() + "/test.xml");
        this.mMiuiCommonCloudHelper.registerObserver(new MiuiCommonCloudHelperStub.Observer() { // from class: com.android.server.powerconsumpiton.PowerConsumptionServiceCouldData.1
            public void update() {
                PowerConsumptionServiceCouldData.this.updateActivityPolicies();
                PowerConsumptionServiceCouldData.this.updateWindowPolicies();
            }
        });
        this.mMiuiCommonCloudHelper.initFromAssets(POWER_CONSUMPTION_CLOUD_FILE);
        this.mMiuiCommonCloudHelper.startCloudObserve();
    }

    public HashMap<String, int[]> getActivityPolicy() {
        return this.mActivityPolicies;
    }

    public HashMap<String, int[]> getWindowPolicies() {
        return this.mWindowPolicies;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActivityPolicies() {
        DeclineWhiteInfo declineWhiteInfo = (DeclineWhiteInfo) this.mMiuiCommonCloudHelper.getDataByModuleName("decline_control");
        if (declineWhiteInfo != null) {
            this.mActivityPolicies.clear();
            Iterator<String> it = declineWhiteInfo.getDeclineWhiteConfigList().iterator();
            while (it.hasNext()) {
                String data = it.next();
                if (data != null) {
                    String[] dataArray = data.split(",");
                    String activity = dataArray[0];
                    int policy = Integer.parseInt(dataArray[1], 2);
                    this.mActivityPolicies.put(activity, new int[]{policy});
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWindowPolicies() {
        ActivityDimInfo activityDimInfo = (ActivityDimInfo) this.mMiuiCommonCloudHelper.getDataByModuleName("dim_control");
        if (activityDimInfo != null) {
            this.mWindowPolicies.clear();
            Iterator<String> it = activityDimInfo.getActivityDimInfoConfigList().iterator();
            while (it.hasNext()) {
                String data = it.next();
                if (data != null) {
                    String[] dataArray = data.split(",");
                    String window = dataArray[0];
                    int policy = Integer.parseInt(dataArray[1], 2);
                    int dimTime = Integer.parseInt(dataArray[2]);
                    int isReleaseWakeLock = Integer.parseInt(dataArray[3]);
                    this.mWindowPolicies.put(window, new int[]{policy, dimTime, isReleaseWakeLock});
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("  Activities cloud data");
        Set<String> activities = this.mActivityPolicies.keySet();
        for (String activity : activities) {
            int[] policy = this.mActivityPolicies.get(activity);
            pw.println("    " + activity + "; policy is " + Arrays.toString(policy));
        }
        pw.println("  Windows cloud data");
        Set<String> windows = this.mWindowPolicies.keySet();
        for (String window : windows) {
            int[] policy2 = this.mWindowPolicies.get(window);
            pw.println("    " + window + "; policy is " + Arrays.toString(policy2));
        }
    }
}
