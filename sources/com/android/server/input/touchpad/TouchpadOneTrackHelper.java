package com.android.server.input.touchpad;

import android.content.Context;
import com.android.server.input.InputOneTrackUtil;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class TouchpadOneTrackHelper {
    private static final String DOWN_EVENT_NAME = "down";
    private static final String DOWN_EVENT_TRACK_TIP = "899.8.0.1.32956";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String TAG = "TouchpadOneTrackHelper";
    private static final String TRACK_TIP = "tip";
    private static final String UP_EVENT_NAME = "up";
    private static final String UP_EVENT_TRACK_TIP = "899.8.0.1.32957";
    public static final String X_COORDINATE = "x_coordinate";
    public static final String Y_COORDINATE = "y_coordinate";
    private static volatile TouchpadOneTrackHelper sInstance;
    List<String> mCachedEvents = new ArrayList();
    private final Context mContext;
    private static int TYPE_DOWN = 0;
    private static int TYPE_UP = 1;
    private static int MIN_EVENTS_REPORT = 100;

    private TouchpadOneTrackHelper(Context context) {
        this.mContext = context;
    }

    public static TouchpadOneTrackHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (TouchpadOneTrackHelper.class) {
                if (sInstance == null) {
                    sInstance = new TouchpadOneTrackHelper(context);
                }
            }
        }
        return sInstance;
    }

    private JSONObject getTrackJSONObject(int type, int x, int y) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("EVENT_NAME", type == TYPE_DOWN ? DOWN_EVENT_NAME : UP_EVENT_NAME);
            obj.put(TRACK_TIP, type == TYPE_DOWN ? DOWN_EVENT_TRACK_TIP : UP_EVENT_TRACK_TIP);
            obj.put(X_COORDINATE, x);
            obj.put(Y_COORDINATE, y);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public void trackTouchpadEvent(int type, int x, int y) {
        JSONObject obj = getTrackJSONObject(type, x, y);
        this.mCachedEvents.add(obj.toString());
        if (this.mCachedEvents.size() >= MIN_EVENTS_REPORT) {
            List<String> list = new ArrayList<>(this.mCachedEvents);
            InputOneTrackUtil.getInstance(this.mContext).trackTouchpadEvents(list);
            this.mCachedEvents.clear();
        }
    }
}
