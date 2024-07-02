package com.android.server.input.shortcut.singlekeyrule;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import com.android.server.LocalServices;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.android.server.policy.MiuiSingleKeyRule;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.AccessController;
import com.miui.server.input.util.ShortCutActionsUtils;

/* loaded from: classes.dex */
public class CameraKeyRule extends MiuiSingleKeyRule {
    private final Context mContext;
    private final Handler mHandler;
    private final String mLongPressAction;
    private final WindowManagerPolicy mWindowManagerPolicy;

    public CameraKeyRule(Context context, Handler handler, MiuiSingleKeyInfo miuiSingleKeyInfo, int currentUserId) {
        super(context, handler, miuiSingleKeyInfo, currentUserId);
        this.mContext = context;
        this.mHandler = handler;
        this.mLongPressAction = miuiSingleKeyInfo.getActionMapForType().get(MiuiSingleKeyRule.ACTION_TYPE_LONG_PRESS);
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiLongPress(long eventTime) {
        triggerLongPress();
    }

    private void triggerLongPress() {
        WindowManagerPolicy.WindowState windowState = null;
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            windowState = baseMiuiPhoneWindowManager.getFocusedWindow();
        }
        if (windowState != null && !AccessController.PACKAGE_CAMERA.equals(windowState.getOwningPackage())) {
            String longPressCameraFunction = getFunction(this.mLongPressAction);
            postTriggerFunction(ShortCutActionsUtils.REASON_OF_LONG_PRESS_CAMERA_KEY, longPressCameraFunction, null, true);
        }
    }

    public boolean postTriggerFunction(final String action, final String function, final Bundle bundle, final boolean hapticFeedback) {
        return this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.CameraKeyRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CameraKeyRule.this.lambda$postTriggerFunction$0(function, action, bundle, hapticFeedback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$postTriggerFunction$0(String function, String action, Bundle bundle, boolean hapticFeedback) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, action, bundle, hapticFeedback);
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    public String getFunction(String action) {
        if ("volumekey_launch_camera".equals(action)) {
            String doubleClickVolumeDownStatus = getObserver().getFunction("volumekey_launch_camera");
            String longPressCameraFunction = MiuiShortcutTriggerHelper.getDoubleVolumeDownKeyFunction(doubleClickVolumeDownStatus);
            if ("launch_camera_and_take_photo".equals(longPressCameraFunction)) {
                return "launch_camera_and_take_photo";
            }
            return "launch_camera";
        }
        return getObserver().getFunction(ShortCutActionsUtils.REASON_OF_LONG_PRESS_CAMERA_KEY);
    }
}
