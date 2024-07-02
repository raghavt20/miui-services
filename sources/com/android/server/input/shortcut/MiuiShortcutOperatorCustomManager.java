package com.android.server.input.shortcut;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.input.MiuiInputThread;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import java.util.List;
import miui.hardware.input.InputFeature;

/* loaded from: classes.dex */
public class MiuiShortcutOperatorCustomManager {
    private static final String RO_MIUI_CUSTOMIZED_REGION = "ro.miui.customized.region";
    private static final String RO_PRODUCT_DEVICE = "ro.product.mod_device";
    private static final String SOFT_BACK_CUSTOMIZED_REGION = "jp_sb";
    private static final List<String> SOFT_BACK_OPERATOR_DEVICE_NAME_LIST = List.of("lilac_jp_sb_global");
    private static volatile MiuiShortcutOperatorCustomManager sMiuiShortcutOperatorCustomManager;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId;
    private final Handler mHandler = MiuiInputThread.getHandler();
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;

    private MiuiShortcutOperatorCustomManager(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mMiuiShortcutTriggerHelper = MiuiShortcutTriggerHelper.getInstance(context);
    }

    public static MiuiShortcutOperatorCustomManager getInstance(Context context) {
        if (sMiuiShortcutOperatorCustomManager == null) {
            synchronized (MiuiShortcutOperatorCustomManager.class) {
                if (sMiuiShortcutOperatorCustomManager == null) {
                    sMiuiShortcutOperatorCustomManager = new MiuiShortcutOperatorCustomManager(context);
                }
            }
        }
        return sMiuiShortcutOperatorCustomManager;
    }

    public void initShortcut() {
        setDefaultCustomFunction(Settings.Secure.getStringForUser(this.mContentResolver, "emergency_gesture_sound_enabled", this.mCurrentUserId), "emergency_gesture_sound_enabled", null, 1, "");
    }

    private void setDefaultCustomFunction(String currentFunction, String action, String function, int global, String region) {
        if (this.mMiuiShortcutTriggerHelper.isLegalData(global, region) && currentFunction == null && "emergency_gesture_sound_enabled".equals(action) && isSoftBankCustom()) {
            Settings.Secure.putIntForUser(this.mContentResolver, action, 1, this.mCurrentUserId);
        }
    }

    public boolean hasOperatorCustomFunctionForMiuiRule(String currentFunction, String action) {
        if ("double_click_power_key".equals(action) && isKDDIOperator()) {
            Settings.System.putStringForUser(this.mContentResolver, action, "au_pay", this.mCurrentUserId);
            return true;
        }
        return false;
    }

    private boolean isSoftBankCustom() {
        return SOFT_BACK_OPERATOR_DEVICE_NAME_LIST.contains(SystemProperties.get(RO_PRODUCT_DEVICE, "")) && SOFT_BACK_CUSTOMIZED_REGION.equals(SystemProperties.get(RO_MIUI_CUSTOMIZED_REGION, ""));
    }

    public boolean isKDDIOperator() {
        return InputFeature.IS_SUPPORT_KDDI;
    }

    public void onUserSwitch(int newUserId) {
        this.mCurrentUserId = newUserId;
    }
}
