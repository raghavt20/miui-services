package com.android.server.input.fling;

import android.content.Context;
import com.android.server.input.InputOneTrackUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FlingOneTrackHelper {
    public static final String ALL_APP_DOWN_NUMBER = "all_app_down_number";
    public static final String ALL_APP_FLING_NUMBER = "all_app_fling_number";
    private static final String ALL_APP_FLING_TRACK_TIP = "1257.0.0.0.31520";
    public static final String APP_FLING_INFO = "app_fling_info";
    private static final String APP_FLING_TRACK_TIP = "1257.0.0.0.31519";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String FLING_TRACK_TIP = "tip";
    public static final String STATISTICS_TRACK_TYPE = "statistics";
    private static final String TAG = "FlingOneTrackHelper";
    private static volatile FlingOneTrackHelper sInstance;
    private final Context mContext;

    private FlingOneTrackHelper(Context context) {
        this.mContext = context;
    }

    public static FlingOneTrackHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (FlingOneTrackHelper.class) {
                if (sInstance == null) {
                    sInstance = new FlingOneTrackHelper(context);
                }
            }
        }
        return sInstance;
    }

    private JSONObject getTrackJSONObject(String type, String tip) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("EVENT_NAME", type);
            obj.put(FLING_TRACK_TIP, tip);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public void trackAppFlingEvent(JSONArray array) {
        JSONObject obj = getTrackJSONObject(STATISTICS_TRACK_TYPE, APP_FLING_TRACK_TIP);
        try {
            obj.put(APP_FLING_INFO, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        InputOneTrackUtil.getInstance(this.mContext).trackFlingEvent(obj.toString());
    }

    public void trackAllAppFlingEvent(int downTimes, int flingTimes) {
        JSONObject obj = getTrackJSONObject(STATISTICS_TRACK_TYPE, ALL_APP_FLING_TRACK_TIP);
        try {
            obj.put(ALL_APP_DOWN_NUMBER, downTimes);
            obj.put(ALL_APP_FLING_NUMBER, flingTimes);
            InputOneTrackUtil.getInstance(this.mContext).trackFlingEvent(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
