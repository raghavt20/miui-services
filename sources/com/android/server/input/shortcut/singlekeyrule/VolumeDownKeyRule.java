package com.android.server.input.shortcut.singlekeyrule;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import com.android.server.LocalServices;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.android.server.policy.MiuiSingleKeyRule;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.util.Map;

/* loaded from: classes.dex */
public class VolumeDownKeyRule extends MiuiSingleKeyRule {
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_CLOSE = 0;
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_LAUNCH_CAMERA = 1;
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_LAUNCH_CAMERA_AND_TAKE_PHOTO = 2;
    private AudioManager mAudioManager;
    private final Context mContext;
    private final Handler mHandler;
    private final Map<String, Integer> mMaxCountMap;
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private int mPolicyFlag;
    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mVolumeKeyWakeLock;
    private final WindowManagerPolicy mWindowManagerPolicy;

    public VolumeDownKeyRule(Context context, Handler handler, MiuiSingleKeyInfo miuiSingleKeyInfo, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper, int currentUserId) {
        super(context, handler, miuiSingleKeyInfo, currentUserId);
        this.mContext = context;
        this.mHandler = handler;
        this.mMaxCountMap = miuiSingleKeyInfo.getActionMaxCountMap();
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mVolumeKeyWakeLock = powerManager.newWakeLock(1, "MiuiKeyShortcutTrigger.mVolumeKeyWakeLock");
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mMiuiShortcutTriggerHelper = miuiShortcutTriggerHelper;
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected boolean miuiSupportLongPress() {
        return false;
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiMultiPress(long downTime, int count) {
        if (count == 2) {
            volumeDownKeyDoubleClick();
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected int getMiuiMaxMultiPressCount() {
        if (isEnableLaunchCamera()) {
            return this.mMaxCountMap.get("volumekey_launch_camera").intValue();
        }
        return 1;
    }

    private boolean isEnableLaunchCamera() {
        String function = getFunction("volumekey_launch_camera");
        int volumeDownLaunchCameraStatus = TextUtils.isEmpty(function) ? 0 : Integer.parseInt(function);
        return volumeDownLaunchCameraStatus == 1 || volumeDownLaunchCameraStatus == 2;
    }

    private void volumeDownKeyDoubleClick() {
        boolean isInjected = (this.mPolicyFlag & BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT) != 0;
        boolean keyguardNotActive = true;
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            keyguardNotActive = baseMiuiPhoneWindowManager.isKeyGuardNotActive();
        }
        boolean isScreenOn = this.mWindowManagerPolicy.isScreenOn();
        if (isInjected || (isScreenOn && keyguardNotActive)) {
            MiuiInputLog.major("volume down key launch fail,isInjected=" + isInjected + " keyguardNotActive=" + keyguardNotActive + " isScreenOn=" + isScreenOn);
        } else if (!isAudioActive()) {
            String function = getFunction("volumekey_launch_camera");
            this.mVolumeKeyWakeLock.acquire(5000L);
            postTriggerFunction(ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_VOLUME_DOWN, MiuiShortcutTriggerHelper.getDoubleVolumeDownKeyFunction(function), null, true);
        }
    }

    private void postTriggerFunction(final String action, final String function, final Bundle bundle, final boolean hapticFeedback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.VolumeDownKeyRule$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                VolumeDownKeyRule.this.lambda$postTriggerFunction$0(function, action, bundle, hapticFeedback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$postTriggerFunction$0(String function, String action, Bundle bundle, boolean hapticFeedback) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, action, bundle, hapticFeedback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updatePolicyFlag$1(int policyFlags) {
        this.mPolicyFlag = policyFlags;
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    public void updatePolicyFlag(final int policyFlags) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.VolumeDownKeyRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VolumeDownKeyRule.this.lambda$updatePolicyFlag$1(policyFlags);
            }
        });
    }

    private boolean isAudioActive() {
        boolean active = false;
        int mode = getAudioManager().getMode();
        if (mode > 0 && mode < 7) {
            MiuiInputLog.defaults("isAudioActive():true");
            return true;
        }
        int size = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < size && (1 == i || !(active = AudioSystem.isStreamActive(i, 0))); i++) {
        }
        if (active) {
            MiuiInputLog.defaults("isAudioActive():" + active);
        }
        return active;
    }

    private AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        return this.mAudioManager;
    }
}
