package com.android.server.input.shortcut.singlekeyrule;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.server.LocalServices;
import com.android.server.policy.MiuiShortcutObserver;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.android.server.policy.MiuiSingleKeyRule;
import com.android.server.policy.OriginalPowerKeyRuleBridge;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.util.Map;

/* loaded from: classes.dex */
public class PowerKeyRule extends MiuiSingleKeyRule implements MiuiShortcutObserver.MiuiShortcutListener {
    private static final int LAUNCH_AU_PAY_WHEN_SOS_ENABLE_DELAY_TIME = 500;
    private static final boolean SUPPORT_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private static final String TAG = "PowerKeyRule";
    private final Map<String, Integer> mActionMaxCountMap;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId;
    private final long mGlobalActionKeyTimeout;
    private final Handler mHandler;
    private boolean mIsPowerGuideTriggeredByLongPressPower;
    private volatile boolean mIsXiaoaiServiceTriggeredByLongPressPower;
    private long mLongPressTimeout;
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private final OriginalPowerKeyRuleBridge mOriginalPowerKeyRuleBridge;
    private boolean mShouldSendPowerUpToSmartHome;
    private final WindowManagerPolicy mWindowManagerPolicy;
    private int mXiaoaiPowerGuideCount;

    public PowerKeyRule(Context context, Handler handler, MiuiSingleKeyInfo miuiSingleKeyInfo, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper, int currentUserId) {
        super(context, handler, miuiSingleKeyInfo, currentUserId);
        this.mXiaoaiPowerGuideCount = 0;
        this.mHandler = handler;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mActionMaxCountMap = miuiSingleKeyInfo.getActionMaxCountMap();
        this.mCurrentUserId = currentUserId;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mOriginalPowerKeyRuleBridge = new OriginalPowerKeyRuleBridge();
        this.mMiuiShortcutTriggerHelper = miuiShortcutTriggerHelper;
        this.mGlobalActionKeyTimeout = ViewConfiguration.get(context).getDeviceGlobalActionKeyTimeout();
        this.mLongPressTimeout = miuiSingleKeyInfo.getLongPressTimeOut();
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    public void init() {
        super.init();
        registerShortcutListener(this);
        this.mMiuiShortcutTriggerHelper.setVeryLongPressPowerBehavior();
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiPress(long downTime) {
        this.mOriginalPowerKeyRuleBridge.onPress(downTime);
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiKeyDown(KeyEvent event) {
        if (this.mWindowManagerPolicy != null && this.mMiuiShortcutTriggerHelper.isTorchEnabled() && !this.mMiuiShortcutTriggerHelper.getPowerManager().isInteractive()) {
            this.mWindowManagerPolicy.setPowerKeyHandled(true);
            Bundle torchBundle = new Bundle();
            torchBundle.putBoolean(ShortCutActionsUtils.EXTRA_TORCH_ENABLED, false);
            torchBundle.putBoolean(ShortCutActionsUtils.EXTRA_SKIP_TELECOM_CHECK, true);
            postTriggerFunction(ShortCutActionsUtils.REASON_OF_TRIGGERED_TORCH_BY_POWER, "turn_on_torch", torchBundle, true);
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiLongPress(long eventTime) {
        boolean triggered = false;
        if (supportMiuiLongPress()) {
            this.mWindowManagerPolicy.setPowerKeyHandled(true);
            triggered = triggerLongPress();
        }
        if (!triggered) {
            this.mOriginalPowerKeyRuleBridge.onLongPress(eventTime);
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiVeryLongPress(long eventTime) {
        this.mOriginalPowerKeyRuleBridge.onVeryLongPress(eventTime);
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiMultiPress(long downTime, int count) {
        boolean triggered = triggerMultiPress(count);
        if (!triggered) {
            this.mOriginalPowerKeyRuleBridge.onMultiPress(downTime, count);
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiLongPressKeyUp(KeyEvent event) {
        workAtPowerUp();
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected boolean miuiSupportVeryLongPress() {
        return supportMiuiLongPress();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.policy.MiuiSingleKeyRule
    public long getMiuiLongPressTimeoutMs() {
        return !supportMiuiLongPress() ? this.mGlobalActionKeyTimeout : this.mLongPressTimeout;
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected int getMiuiMaxMultiPressCount() {
        int maxPressCount = 1;
        for (Map.Entry<String, Integer> actionMaxCountEntry : this.mActionMaxCountMap.entrySet()) {
            String function = getFunction(actionMaxCountEntry.getKey());
            if (!TextUtils.isEmpty(function) && !"none".equals(function) && actionMaxCountEntry.getValue().intValue() >= maxPressCount) {
                maxPressCount = actionMaxCountEntry.getValue().intValue();
            }
        }
        if (this.mMiuiShortcutTriggerHelper.shouldLaunchSos()) {
            return 5;
        }
        return maxPressCount;
    }

    private boolean supportMiuiLongPress() {
        if (!this.mMiuiShortcutTriggerHelper.isUserSetUpComplete()) {
            Slog.d(TAG, "user set up not complete");
            return false;
        }
        if (this.mMiuiShortcutTriggerHelper.isSupportLongPressPowerGuide() || longPressPowerIsTrigger()) {
            return true;
        }
        String function = getFunction("long_press_power_key");
        return (TextUtils.isEmpty(function) || "none".equals(function)) ? false : true;
    }

    private boolean isSupportRsa() {
        return this.mMiuiShortcutTriggerHelper.supportRSARegion() && this.mMiuiShortcutTriggerHelper.getRSAGuideStatus() == 1;
    }

    private boolean triggerLongPress() {
        String function = getFunction("long_press_power_key");
        if ("launch_google_search".equals(function)) {
            if (!isSupportRsa() || this.mMiuiShortcutTriggerHelper.isCtsMode()) {
                boolean intercept = postTriggerFunction("long_press_power_key", function, null, true);
                return intercept;
            }
            boolean intercept2 = postTriggerPowerGuide("long_press_power_key", function);
            return intercept2;
        }
        if ("launch_voice_assistant".equals(function)) {
            Bundle bundle = new Bundle();
            bundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
            bundle.putBoolean(ShortCutActionsUtils.EXTRA_POWER_GUIDE, false);
            boolean intercept3 = postTriggerFunction("long_press_power_key", function, bundle, true);
            if (intercept3) {
                changeXiaoAiPowerGuideStatus("long_press_power_key");
                return intercept3;
            }
            return intercept3;
        }
        if ("launch_smarthome".equals(function)) {
            this.mShouldSendPowerUpToSmartHome = true;
            Bundle smartHomeBundle = new Bundle();
            smartHomeBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
            boolean intercept4 = postTriggerFunction("long_press_power_key", function, smartHomeBundle, true);
            return intercept4;
        }
        if (!"none".equals(function) && !TextUtils.isEmpty(function)) {
            return false;
        }
        boolean intercept5 = postTriggerPowerGuide("long_press_power_key", function);
        return intercept5;
    }

    private boolean postTriggerPowerGuide(String action, String function) {
        if (!this.mMiuiShortcutTriggerHelper.isSupportLongPressPowerGuide()) {
            return false;
        }
        if (MiuiShortcutTriggerHelper.CURRENT_DEVICE_REGION.equals("cn")) {
            Bundle voiceBundle = new Bundle();
            voiceBundle.putBoolean(ShortCutActionsUtils.EXTRA_POWER_GUIDE, true);
            voiceBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
            boolean result = postTriggerFunction(action, "launch_voice_assistant", voiceBundle, true);
            if (result) {
                changeXiaoAiPowerGuideStatus(action);
                return result;
            }
            return result;
        }
        boolean result2 = postTriggerFunction(action, "launch_global_power_guide", null, true);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "global_power_guide", 0, this.mCurrentUserId);
        this.mIsPowerGuideTriggeredByLongPressPower = true;
        this.mMiuiShortcutTriggerHelper.setLongPressPowerBehavior(function);
        return result2;
    }

    private boolean postTriggerFunction(final String action, final String function, final Bundle bundle, final boolean hapticFeedback) {
        return this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.PowerKeyRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PowerKeyRule.this.lambda$postTriggerFunction$0(function, action, bundle, hapticFeedback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$postTriggerFunction$0(String function, String action, Bundle bundle, boolean hapticFeedback) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, action, bundle, hapticFeedback);
    }

    private void changeXiaoAiPowerGuideStatus(String action) {
        if ("long_press_power_key".equals(action)) {
            if (this.mMiuiShortcutTriggerHelper.mXiaoaiPowerGuideFlag == 1) {
                int i = this.mXiaoaiPowerGuideCount + 1;
                this.mXiaoaiPowerGuideCount = i;
                if (i >= 2) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "xiaoai_power_guide", 0, this.mCurrentUserId);
                }
            }
            this.mIsXiaoaiServiceTriggeredByLongPressPower = true;
            return;
        }
        if (ShortCutActionsUtils.ACTION_POWER_UP.equals(action)) {
            this.mIsXiaoaiServiceTriggeredByLongPressPower = false;
        }
    }

    private void workAtPowerUp() {
        if (this.mIsXiaoaiServiceTriggeredByLongPressPower) {
            Bundle voiceBundle = new Bundle();
            voiceBundle.putBoolean(ShortCutActionsUtils.EXTRA_POWER_GUIDE, false);
            voiceBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, ShortCutActionsUtils.ACTION_POWER_UP);
            voiceBundle.putBoolean(ShortCutActionsUtils.DISABLE_SHORTCUT_TRACK, true);
            boolean triggered = postTriggerFunction("long_press_power_key", "launch_voice_assistant", voiceBundle, true);
            if (triggered) {
                changeXiaoAiPowerGuideStatus(ShortCutActionsUtils.ACTION_POWER_UP);
            }
        }
        if (this.mShouldSendPowerUpToSmartHome) {
            Bundle smartHomeBundle = new Bundle();
            smartHomeBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, ShortCutActionsUtils.ACTION_POWER_UP);
            smartHomeBundle.putBoolean(ShortCutActionsUtils.DISABLE_SHORTCUT_TRACK, true);
            boolean triggered2 = postTriggerFunction("long_press_power_key", "launch_smarthome", smartHomeBundle, true);
            if (triggered2) {
                this.mShouldSendPowerUpToSmartHome = false;
            }
        }
        if (this.mMiuiShortcutTriggerHelper.isSupportLongPressPowerGuide()) {
            this.mMiuiShortcutTriggerHelper.setVeryLongPressPowerBehavior();
            this.mIsPowerGuideTriggeredByLongPressPower = false;
        }
    }

    private boolean longPressPowerIsTrigger() {
        return this.mIsPowerGuideTriggeredByLongPressPower || this.mIsXiaoaiServiceTriggeredByLongPressPower || this.mShouldSendPowerUpToSmartHome;
    }

    private boolean triggerMultiPress(int count) {
        switch (count) {
            case 2:
                return triggerDoubleClick();
            case 5:
                return triggerFiveClick();
            default:
                this.mHandler.removeCallbacks(doubleClickPowerRunnable());
                return false;
        }
    }

    private boolean triggerFiveClick() {
        if (this.mMiuiShortcutTriggerHelper.shouldLaunchSosByType(1)) {
            return postTriggerFunction("five_tap_power", "launch_sos", null, false);
        }
        return false;
    }

    private boolean triggerDoubleClick() {
        if (SUPPORT_POWERFP && inFingerprintEnrolling()) {
            Slog.i(TAG, "Power button double tap gesture detected, but in FingerprintEnrolling, return.");
            return false;
        }
        String function = getFunction("double_click_power_key");
        Bundle bundle = null;
        if (needDelayTriggerFunction("double_click_power_key")) {
            if (this.mMiuiShortcutTriggerHelper.getPowerManager().isInteractive()) {
                this.mHandler.postDelayed(doubleClickPowerRunnable(), 500L);
            }
        } else {
            if ("launch_smarthome".equals(function)) {
                bundle = new Bundle();
                bundle.putString(ShortCutActionsUtils.EXTRA_ACTION_SOURCE, "double_click_power_key");
            }
            postTriggerFunction(getTriggerDoubleClickReason(function), function, bundle, true);
        }
        return (TextUtils.isEmpty(function) || "none".equals(function)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public String getTriggerDoubleClickReason(String function) {
        char c;
        if (TextUtils.isEmpty(function)) {
            return "double_click_power_key";
        }
        switch (function.hashCode()) {
            case -1534821982:
                if (function.equals("google_pay")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1406946787:
                if (function.equals("au_pay")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1074479227:
                if (function.equals("mi_pay")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1736809777:
                if (function.equals("launch_camera")) {
                    c = 3;
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
            case 2:
                return "double_click_power";
            case 3:
                return "power_double_tap";
            default:
                return "double_click_power_key";
        }
    }

    private boolean inFingerprintEnrolling() {
        String topClassName;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        try {
            topClassName = am.getRunningTasks(1).get(0).topActivity.getClassName();
        } catch (Exception e) {
            Slog.e(TAG, "Exception", e);
        }
        return "com.android.settings.NewFingerprintInternalActivity".equals(topClassName);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean needDelayTriggerFunction(String action) {
        char c;
        String function = getFunction(action);
        if (TextUtils.isEmpty(function)) {
            return false;
        }
        String function2 = getFunction(action);
        switch (function2.hashCode()) {
            case -1534821982:
                if (function2.equals("google_pay")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1406946787:
                if (function2.equals("au_pay")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1074479227:
                if (function2.equals("mi_pay")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1736809777:
                if (function2.equals("launch_camera")) {
                    c = 3;
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
            case 2:
            case 3:
                return this.mMiuiShortcutTriggerHelper.shouldLaunchSos();
            default:
                return false;
        }
    }

    private Runnable doubleClickPowerRunnable() {
        return new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.PowerKeyRule.1
            @Override // java.lang.Runnable
            public void run() {
                String doubleClickFunction = PowerKeyRule.this.getFunction("double_click_power_key");
                ShortCutActionsUtils.getInstance(PowerKeyRule.this.mContext).triggerFunction(doubleClickFunction, PowerKeyRule.this.getTriggerDoubleClickReason(doubleClickFunction), null, false);
            }
        };
    }

    @Override // com.android.server.policy.MiuiShortcutObserver.MiuiShortcutListener
    public void onSingleChanged(MiuiSingleKeyRule rule, Uri uri) {
        if (rule.getPrimaryKey() == 26 && Settings.System.getUriFor("long_press_power_key").equals(uri)) {
            this.mMiuiShortcutTriggerHelper.setVeryLongPressPowerBehavior();
            this.mMiuiShortcutTriggerHelper.setLongPressPowerBehavior(rule.getFunction("long_press_power_key"));
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mCurrentUserId = currentUserId;
        super.onUserSwitch(currentUserId, isNewUser);
    }
}
