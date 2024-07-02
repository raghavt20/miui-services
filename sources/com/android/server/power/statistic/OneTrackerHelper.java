package com.android.server.power.statistic;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Slog;

/* loaded from: classes.dex */
public class OneTrackerHelper {
    private static final String APP_ID = "APP_ID";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String PACKAGE = "PACKAGE";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    public static final String TAG = "Power_OneTrackerHelper";
    private static final String TRACK_EVENT = "onetrack.action.TRACK_EVENT";
    private Context mContext;

    public OneTrackerHelper(Context context) {
        this.mContext = context;
    }

    public Intent getTrackEventIntent(String appId, String eventName, String packageName) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra("APP_ID", appId);
        intent.putExtra("PACKAGE", packageName);
        intent.putExtra("EVENT_NAME", eventName);
        intent.setFlags(3);
        return intent;
    }

    public void reportTrackEventByIntent(Intent intent) {
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "send one tracker by intent fail! " + e);
        }
    }
}
