package com.miui.server;

import android.content.Context;
import android.database.ContentObserver;
import android.inputmethodservice.MiuiBottomConfig;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import miui.util.CompatibilityHelper;

/* loaded from: classes.dex */
public class InputMethodHelper {
    private static final String DEFAULT_TAOBAO_CMD_RULE = "[￥€$¢₳₤₴][a-zA-Z0-9]+[￥€$¢₳₤₴]";
    private static final String INPUT_METHOD_TAOBAO_CMD_ENABLE = "input_method_taobao_cmd_enable";
    private static final String INPUT_METHOD_TAOBAO_CMD_MODULE_NAME = "InputMethodTaobaoCmdModule";
    private static final String INPUT_METHOD_TAOBAO_CMD_RULE = "input_method_taobao_cmd_rule";
    private static final String TAG = "InputMethodHelper";
    private static final String TAOBAO_RULE_ENABLE_KEY = "EnableTbCmd";
    private static final String TAOBAO_RULE_TEXT_KEY = "TbCmdRule";
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");

    public static void init(Context context) {
        if (!isSupportMiuiBottom()) {
            Log.i(TAG, "Not support miui bottom.");
            return;
        }
        registerContentObserver(context);
        initInputMethodTbCmdRule(context);
        initForUser(context, 0);
    }

    private static boolean isFullScreenDevice() {
        try {
            return CompatibilityHelper.hasNavigationBar(0);
        } catch (RemoteException e) {
            Log.e(TAG, "get isFullScreenDevice error", e);
            return false;
        }
    }

    private static boolean isMiuiBottomNeedSet(Context context, int userId) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "enable_miui_ime_bottom_view", -1, userId) == -1;
    }

    private static void initForUser(Context context, int userId) {
        if (!isMiuiBottomNeedSet(context, userId)) {
            return;
        }
        if (!isFullScreenDevice() || MiuiBottomConfig.sBigChinDevices.contains(Build.DEVICE) || miui.os.Build.IS_INTERNATIONAL_BUILD) {
            Log.d(TAG, "disable miui bottom for user " + userId);
            Settings.Secure.putIntForUser(context.getContentResolver(), "enable_miui_ime_bottom_view", 0, userId);
        } else {
            Settings.Secure.putIntForUser(context.getContentResolver(), "enable_miui_ime_bottom_view", 1, userId);
            Log.d(TAG, "enable miui bottom " + userId);
        }
    }

    private static boolean isSupportMiuiBottom() {
        return SystemProperties.getInt("ro.miui.support_miui_ime_bottom", 0) == 1;
    }

    private static void registerContentObserver(Context context) {
        InputMethodTbCmdRuleObserver tbCmdRuleObserver = new InputMethodTbCmdRuleObserver(BackgroundThread.getHandler(), context);
        context.getContentResolver().registerContentObserver(URI_CLOUD_ALL_DATA_NOTIFY, true, tbCmdRuleObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class InputMethodTbCmdRuleObserver extends ContentObserver {
        private Context mContext;

        public InputMethodTbCmdRuleObserver(Handler handler, Context context) {
            super(handler);
            this.mContext = context;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            InputMethodHelper.initInputMethodTbCmdRule(this.mContext);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void initInputMethodTbCmdRule(Context context) {
        Settings.Global.putInt(context.getContentResolver(), INPUT_METHOD_TAOBAO_CMD_ENABLE, MiuiSettings.SettingsCloudData.getCloudDataBoolean(context.getContentResolver(), INPUT_METHOD_TAOBAO_CMD_MODULE_NAME, TAOBAO_RULE_ENABLE_KEY, true) ? 1 : 0);
        Settings.Global.putString(context.getContentResolver(), INPUT_METHOD_TAOBAO_CMD_RULE, MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), INPUT_METHOD_TAOBAO_CMD_MODULE_NAME, TAOBAO_RULE_TEXT_KEY, DEFAULT_TAOBAO_CMD_RULE));
    }
}
