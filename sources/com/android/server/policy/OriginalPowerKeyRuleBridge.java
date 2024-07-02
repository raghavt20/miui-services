package com.android.server.policy;

import com.android.server.LocalServices;
import com.android.server.policy.PhoneWindowManager;

/* loaded from: classes.dex */
public class OriginalPowerKeyRuleBridge {
    private PhoneWindowManager.PowerKeyRule mOriginalPowerKeyRule;

    public OriginalPowerKeyRuleBridge() {
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            this.mOriginalPowerKeyRule = baseMiuiPhoneWindowManager.getOriginalPowerKeyRule();
        }
    }

    public void onPress(long downTime) {
        PhoneWindowManager.PowerKeyRule powerKeyRule = this.mOriginalPowerKeyRule;
        if (powerKeyRule != null) {
            powerKeyRule.onPress(downTime);
        }
    }

    public void onLongPress(long eventTime) {
        PhoneWindowManager.PowerKeyRule powerKeyRule = this.mOriginalPowerKeyRule;
        if (powerKeyRule != null) {
            powerKeyRule.onLongPress(eventTime);
        }
    }

    public void onVeryLongPress(long eventTime) {
        PhoneWindowManager.PowerKeyRule powerKeyRule = this.mOriginalPowerKeyRule;
        if (powerKeyRule != null) {
            powerKeyRule.onVeryLongPress(eventTime);
        }
    }

    public void onMultiPress(long downTime, int count) {
        PhoneWindowManager.PowerKeyRule powerKeyRule = this.mOriginalPowerKeyRule;
        if (powerKeyRule != null) {
            powerKeyRule.onMultiPress(downTime, count);
        }
    }
}
