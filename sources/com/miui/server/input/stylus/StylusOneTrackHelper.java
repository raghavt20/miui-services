package com.miui.server.input.stylus;

import android.content.Context;
import android.util.Slog;
import com.android.server.input.InputOneTrackUtil;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class StylusOneTrackHelper {
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String FUNCTION_TRIGGER_TRACK_TYPE = "function_trigger";
    private static final String LASER_TRIGGER_TRACK_TYPE = "laser_trigger";
    private static final String LONG_PRESS_TRACK_TYPE = "long_press";
    private static final String PRESS_TRACK_TYPE = "press";
    private static final String SHORTHAND_TRACK_TYPE = "shorthand";
    private static final String STYLUS_TRACK_APP_PACKAGE_NAME = "app_package_name";
    private static final String STYLUS_TRACK_FUNCTION_TYPE = "function_type";
    private static final String STYLUS_TRACK_KEY_TYPE = "key_type";
    private static final String STYLUS_TRACK_TIP = "tip";
    private static final String STYLUS_TRACK_TIP_FUNCTION_TRIGGER_HIGHLIGHT = "899.2.0.1.28606";
    private static final String STYLUS_TRACK_TIP_FUNCTION_TRIGGER_PEN = "899.2.0.1.28605";
    private static final String STYLUS_TRACK_TIP_LASER_TRIGGER = "899.2.2.1.27097";
    private static final String STYLUS_TRACK_TIP_LONG_PRESS = "899.2.3.1.27099";
    private static final String STYLUS_TRACK_TIP_PRESS = "899.2.3.1.27100";
    private static final String STYLUS_TRACK_TIP_SHORTHAND = "899.2.0.1.29459";
    private static final String TAG = "StylusOneTrackHelper";
    private static volatile StylusOneTrackHelper sInstance;
    private final Context mContext;

    private StylusOneTrackHelper(Context context) {
        this.mContext = context;
    }

    public static StylusOneTrackHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (StylusOneTrackHelper.class) {
                if (sInstance == null) {
                    sInstance = new StylusOneTrackHelper(context);
                }
            }
        }
        return sInstance;
    }

    public void trackStylusLaserTrigger() {
        JSONObject jsonObject = getTrackJSONObject(LASER_TRIGGER_TRACK_TYPE, STYLUS_TRACK_TIP_LASER_TRIGGER);
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    public void trackStylusKeyLongPress(String functionType, String keyType, String packageName) {
        JSONObject jsonObject = getTrackJSONObject(LONG_PRESS_TRACK_TYPE, STYLUS_TRACK_TIP_LONG_PRESS);
        try {
            jsonObject.put(STYLUS_TRACK_FUNCTION_TYPE, functionType);
            jsonObject.put(STYLUS_TRACK_KEY_TYPE, keyType);
            jsonObject.put("app_package_name", packageName);
        } catch (JSONException e) {
            Slog.e(TAG, "Stylus trackEvent data fail!" + e);
        }
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    public void trackStylusKeyPress(String keyType, String packageName) {
        JSONObject jsonObject = getTrackJSONObject(PRESS_TRACK_TYPE, STYLUS_TRACK_TIP_PRESS);
        try {
            jsonObject.put(STYLUS_TRACK_KEY_TYPE, keyType);
            jsonObject.put("app_package_name", packageName);
        } catch (JSONException e) {
            Slog.e(TAG, "Failed to get event tracking data of stylus!" + e);
        }
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    public void trackStylusShortHandTrigger() {
        JSONObject jsonObject = getTrackJSONObject(SHORTHAND_TRACK_TYPE, STYLUS_TRACK_TIP_SHORTHAND);
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    public void trackStylusPenTrigger() {
        JSONObject jsonObject = getTrackJSONObject(FUNCTION_TRIGGER_TRACK_TYPE, STYLUS_TRACK_TIP_FUNCTION_TRIGGER_PEN);
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    public void trackStylusHighLightTrigger() {
        JSONObject jsonObject = getTrackJSONObject(FUNCTION_TRIGGER_TRACK_TYPE, STYLUS_TRACK_TIP_FUNCTION_TRIGGER_HIGHLIGHT);
        InputOneTrackUtil.getInstance(this.mContext).trackStylusEvent(jsonObject.toString());
    }

    private JSONObject getTrackJSONObject(String trackType, String tip) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", trackType);
            jsonObject.put(STYLUS_TRACK_TIP, tip);
        } catch (JSONException e) {
            Slog.e(TAG, "Failed to get event tracking data of stylus!" + e);
        }
        return jsonObject;
    }
}
