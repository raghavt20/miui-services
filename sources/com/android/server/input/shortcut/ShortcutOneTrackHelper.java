package com.android.server.input.shortcut;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.MiInputPhotoHandleManager;
import com.android.server.input.MiuiInputThread;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.miui.server.input.util.ShortCutActionsUtils;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import miui.os.Build;
import miui.util.MiuiMultiDisplayTypeInfo;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ShortcutOneTrackHelper {
    private static final String DEVICE_TYPE_FLIP = "flip";
    private static final String DEVICE_TYPE_FOLD = "fold";
    private static final String DEVICE_TYPE_PAD = "pad";
    private static final String DEVICE_TYPE_PHONE = "phone";
    private static final String EVENT_NAME = "EVENT_NAME";
    private static final String GLOBAL_PRIVATE_KEY_ID = "60d438821b8068f4edd8d1a3d85339eb65402dc6";
    private static final String GLOBAL_PROJECT_ID = "gesture-shortcuts-c097f";
    private static final String GLOBAL_TOPIC = "topic_ods_pubsub_event_di_31000401650";
    private static final String SCREEN_TYPE = "screen_type";
    private static final String SCREEN_TYPE_EXTERNAL_SCREEN = "external_screen";
    private static final String SCREEN_TYPE_INNER_SCREEN = "inner_screen";
    private static final List<String> SHORTCUT_TRACK_FOR_ONE_TRACK = new ArrayList(Arrays.asList("cn", "ru"));
    private static final String SHORTCUT_TRACK_MODE_TYPE = "model_type";
    private static final String SHORTCUT_TRACK_PHONE_TYPE = "phone_type";
    private static final String SHORTCUT_TRACK_TIP = "tip";
    private static final String SHORTCUT_TRACK_TIP_STATUS = "1257.0.0.0.28747";
    private static final String SHORTCUT_TRACK_TIP_TRIGGER = "1257.0.0.0.28748";
    public static final String STATUS_TRACK_TYPE = "status";
    private static final String TAG = "ShortcutOneTrackHelper";
    public static final String TRIGGER_TRACK_TYPE = "trigger";
    private static volatile ShortcutOneTrackHelper sInstance;
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private boolean mFoldStatus;
    private List<String> mShortcutSupportTrackList;
    private final AlarmManager.OnAlarmListener mAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.input.shortcut.ShortcutOneTrackHelper.1
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            ShortcutOneTrackHelper.this.trackShortcutEventStatus();
            ShortcutOneTrackHelper.this.setUploadShortcutAlarm(false);
        }
    };
    private final Handler mHandler = MiuiInputThread.getHandler();

    private ShortcutOneTrackHelper(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        initShortcutTrackList();
    }

    private void initShortcutTrackList() {
        if (this.mShortcutSupportTrackList == null) {
            this.mShortcutSupportTrackList = new ArrayList(Arrays.asList("long_press_power_key", "long_press_home_key", "long_press_menu_key", "long_press_back_key", "screen_key_press_app_switch", "double_click_power_key", "three_gesture_down", "three_gesture_long_press", "volumekey_launch_camera", "back_double_tap", "back_triple_tap", "double_knock", "knock_gesture_v", "knock_slide_shape", "knock_long_press_horizontal_slid", "fingerprint_double_tap", "key_combination_power_volume_down", "key_combination_power_volume_up", "key_combination_power_home", "key_combination_power_menu", "key_combination_power_back", ShortCutActionsUtils.REASON_OF_LONG_PRESS_CAMERA_KEY, "power_double_tap", "double_click_power", ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_VOLUME_DOWN, "very_long_press_power"));
        }
    }

    public boolean supportTrackForCurrentAction(String action) {
        return this.mShortcutSupportTrackList.contains(action);
    }

    public static ShortcutOneTrackHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ShortcutOneTrackHelper.class) {
                if (sInstance == null) {
                    sInstance = new ShortcutOneTrackHelper(context);
                }
            }
        }
        return sInstance;
    }

    public void setUploadShortcutAlarm(boolean init) {
        long nowTime = System.currentTimeMillis();
        long nextTime = (init ? SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION : 86400000L) + nowTime;
        this.mAlarmManager.setExact(1, nextTime, "upload_shortcut_status", this.mAlarmListener, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void trackShortcutEventStatus() {
        JSONObject jsonObject = getTrackJSONObject("status");
        try {
            for (String action : this.mShortcutSupportTrackList) {
                if (!needSkipTrackByAction(action)) {
                    String shortcutTrackAction = getShortcutTrackAction(action);
                    jsonObject.put(shortcutTrackAction, getFunctionByAction(shortcutTrackAction));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        InputOneTrackUtil.getInstance(this.mContext).trackShortcutEvent(jsonObject.toString());
    }

    private boolean needSkipTrackByAction(String action) {
        return "key_combination_power_volume_up".equals(action) || (!MiInputPhotoHandleManager.getInstance(this.mContext).isPhotoHandleHasConnected() && ShortCutActionsUtils.REASON_OF_LONG_PRESS_CAMERA_KEY.equals(action)) || "very_long_press_power".equals(action);
    }

    public void trackShortcutEventTrigger(String action, String function) {
        if (supportTrackForCurrentAction(action)) {
            String shortcutTriggerAction = getShortcutTrackAction(action);
            JSONObject jsonObject = getTrackJSONObject(TRIGGER_TRACK_TYPE);
            try {
                if (MiuiMultiDisplayTypeInfo.isFlipDevice()) {
                    jsonObject.put(SCREEN_TYPE, this.mFoldStatus ? SCREEN_TYPE_EXTERNAL_SCREEN : SCREEN_TYPE_INNER_SCREEN);
                }
                jsonObject.put(shortcutTriggerAction, function);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            InputOneTrackUtil.getInstance(this.mContext).trackShortcutEvent(jsonObject.toString());
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private String getShortcutTrackAction(String action) {
        char c;
        switch (action.hashCode()) {
            case -1708621393:
                if (action.equals("power_double_tap")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1628511870:
                if (action.equals(ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_VOLUME_DOWN)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -490466272:
                if (action.equals("double_click_power")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return "double_click_power_key";
            case 2:
                return "volumekey_launch_camera";
            default:
                return action;
        }
    }

    public static boolean isShortcutTrackForOneTrack() {
        return SHORTCUT_TRACK_FOR_ONE_TRACK.contains(MiuiShortcutTriggerHelper.CURRENT_DEVICE_REGION);
    }

    private JSONObject getTrackJSONObject(String type) {
        String str = "pad";
        JSONObject jsonObject = new JSONObject();
        try {
            if ("status".equals(type)) {
                jsonObject.put("EVENT_NAME", "status");
                jsonObject.put(SHORTCUT_TRACK_TIP, SHORTCUT_TRACK_TIP_STATUS);
            } else if (TRIGGER_TRACK_TYPE.equals(type)) {
                jsonObject.put("EVENT_NAME", TRIGGER_TRACK_TYPE);
                jsonObject.put(SHORTCUT_TRACK_TIP, SHORTCUT_TRACK_TIP_TRIGGER);
            }
            if (!isShortcutTrackForOneTrack()) {
                jsonObject.put("PROJECT_ID", GLOBAL_PROJECT_ID);
                jsonObject.put("TOPIC", GLOBAL_TOPIC);
                jsonObject.put("PRIVATE_KEY_ID", GLOBAL_PRIVATE_KEY_ID);
            }
            boolean isPad = "pad".equals(getDeviceType());
            if (!isPad) {
                str = DEVICE_TYPE_PHONE;
            }
            jsonObject.put(SHORTCUT_TRACK_MODE_TYPE, str);
            jsonObject.put(SHORTCUT_TRACK_PHONE_TYPE, isPad ? null : getDeviceType());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String getDeviceType() {
        if (MiuiMultiDisplayTypeInfo.isFoldDevice()) {
            return DEVICE_TYPE_FOLD;
        }
        if (Build.IS_TABLET) {
            return "pad";
        }
        if (MiuiMultiDisplayTypeInfo.isFlipDevice()) {
            return DEVICE_TYPE_FLIP;
        }
        return DEVICE_TYPE_PHONE;
    }

    private String getFunctionByAction(String action) {
        if ("volumekey_launch_camera".equals(action)) {
            int functionStatus = Settings.System.getIntForUser(this.mContext.getContentResolver(), action, -1, -2);
            return getValidFunctionForValueIsIntType(action, functionStatus);
        }
        String function = Settings.System.getStringForUser(this.mContext.getContentResolver(), action, -2);
        if ("none".equals(function)) {
            return "user_close";
        }
        if (TextUtils.isEmpty(function)) {
            return "default_close";
        }
        return function;
    }

    private String getValidFunctionForValueIsIntType(String action, int function) {
        if ("volumekey_launch_camera".equals(action)) {
            if (function == 1) {
                return "launch_camera";
            }
            if (function == 2) {
                return "launch_camera_and_take_photo";
            }
        } else if ("screen_key_press_app_switch".equals(action)) {
            boolean pressToAppSwitch = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_key_press_app_switch", 1, -2) != 0;
            return pressToAppSwitch ? "launch_recents" : "show_menu";
        }
        if (function == 0) {
            return "user_close";
        }
        return "default_close";
    }

    public void notifyFoldStatus(boolean folded) {
        this.mFoldStatus = folded;
    }
}
