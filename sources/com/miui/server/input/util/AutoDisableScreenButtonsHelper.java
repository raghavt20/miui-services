package com.miui.server.input.util;

import android.content.Context;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import miui.securityspace.CrossUserUtils;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AutoDisableScreenButtonsHelper {
    public static final String CLOUD_SETTING = "auto_disable_screen_button_cloud_setting";
    public static final int ENABLE_ASK = 1;
    public static final int ENABLE_AUTO = 2;
    public static final String MODULE_AUTO_DIS_NAV_BTN = "AutoDisableNavigationButton1";
    public static final int NO = 3;
    public static final int NONE = 0;
    private static final String TAG = "AutoDisableHelper";
    private static JSONObject mCloudJson;
    private static JSONObject mUserJson;

    public static int getAppFlag(Context context, String pkg) {
        Object flagObj = getValue(context, pkg);
        if (flagObj == null) {
            return 3;
        }
        int flag = ((Integer) flagObj).intValue();
        return flag;
    }

    public static Object getValue(Context context, String key) {
        checkJson(context);
        try {
            JSONObject jSONObject = mUserJson;
            if (jSONObject != null && jSONObject.has(key)) {
                return mUserJson.get(key);
            }
            JSONObject jSONObject2 = mCloudJson;
            if (jSONObject2 != null && jSONObject2.has(key)) {
                return mCloudJson.get(key);
            }
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setFlag(Context context, String pkg, int flag) {
        setValue(context, pkg, Integer.valueOf(flag));
    }

    public static void setValue(Context context, String key, Object value) {
        checkJson(context);
        JSONObject jSONObject = mUserJson;
        if (jSONObject != null && context != null) {
            try {
                jSONObject.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MiuiSettings.System.putStringForUser(context.getContentResolver(), "auto_disable_screen_button", mUserJson.toString(), CrossUserUtils.getCurrentUserId());
        }
    }

    public static void updateUserJson(String config) {
        if (!TextUtils.isEmpty(config)) {
            JSONObject jSONObject = mUserJson;
            if (jSONObject != null && config.equals(jSONObject.toString())) {
                return;
            }
            try {
                mUserJson = new JSONObject(config);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateCloudJson(String config) {
        if (!TextUtils.isEmpty(config)) {
            JSONObject jSONObject = mCloudJson;
            if (jSONObject != null && config.equals(jSONObject.toString())) {
                return;
            }
            try {
                mCloudJson = new JSONObject(config);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static void checkJson(Context context) {
        if (context == null) {
            return;
        }
        if (mUserJson == null) {
            String userSetting = MiuiSettings.System.getStringForUser(context.getContentResolver(), "auto_disable_screen_button", CrossUserUtils.getCurrentUserId());
            if (userSetting == null) {
                mUserJson = new JSONObject();
            } else {
                updateUserJson(userSetting);
            }
        }
        if (mCloudJson == null) {
            String cloudConfig = Settings.System.getString(context.getContentResolver(), CLOUD_SETTING);
            updateCloudJson(cloudConfig);
        }
    }
}
