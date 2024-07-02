package com.android.server.input.shortcut.singlekeyrule;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.android.server.policy.MiuiSingleKeyRule;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.util.Map;

/* loaded from: classes.dex */
public class DefaultSingleKeyRule extends MiuiSingleKeyRule {
    private final Map<String, String> mActionMapForType;
    private final Context mContext;
    private final Handler mHandler;
    private String mLongPressFunction;
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private final int mPrimaryKey;

    public DefaultSingleKeyRule(Context context, Handler handler, MiuiSingleKeyInfo miuiSingleKeyInfo, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper, int currentUserId) {
        super(context, handler, miuiSingleKeyInfo, currentUserId);
        this.mContext = context;
        this.mHandler = handler;
        this.mPrimaryKey = miuiSingleKeyInfo.getPrimaryKey();
        this.mMiuiShortcutTriggerHelper = miuiShortcutTriggerHelper;
        this.mActionMapForType = miuiSingleKeyInfo.getActionMapForType();
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected void onMiuiLongPress(long eventTime) {
        if (!TextUtils.isEmpty(this.mLongPressFunction) && !"none".equals(this.mLongPressFunction)) {
            triggerFunction(this.mLongPressFunction, this.mActionMapForType.get(MiuiSingleKeyRule.ACTION_TYPE_LONG_PRESS));
        }
    }

    @Override // com.android.server.policy.MiuiSingleKeyRule
    protected boolean miuiSupportLongPress() {
        String function = getFunction(this.mActionMapForType.get(MiuiSingleKeyRule.ACTION_TYPE_LONG_PRESS));
        this.mLongPressFunction = function;
        return (TextUtils.isEmpty(function) || "none".equals(this.mLongPressFunction) || !this.mMiuiShortcutTriggerHelper.supportAOSPTriggerFunction(this.mPrimaryKey)) ? false : true;
    }

    private void postTriggerFunction(final String function, final String action, final Bundle bundle, final boolean hapticFeedback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.singlekeyrule.DefaultSingleKeyRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DefaultSingleKeyRule.this.lambda$postTriggerFunction$0(function, action, bundle, hapticFeedback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$postTriggerFunction$0(String function, String action, Bundle bundle, boolean hapticFeedback) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, action, bundle, hapticFeedback);
    }

    private void triggerFunction(String function, String action) {
        if (82 == this.mPrimaryKey && !this.mMiuiShortcutTriggerHelper.isPressToAppSwitch() && "none".equals(getFunction(this.mActionMapForType.get(MiuiSingleKeyRule.ACTION_TYPE_LONG_PRESS)))) {
            return;
        }
        Bundle bundle = null;
        if ("close_app".equals(function)) {
            bundle = new Bundle();
            bundle.putString(ShortCutActionsUtils.EXTRA_SHORTCUT_TYPE, ShortCutActionsUtils.TYPE_PHONE_SHORTCUT);
        }
        postTriggerFunction(function, action, bundle, true);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.policy.MiuiSingleKeyRule
    public long getMiuiLongPressTimeoutMs() {
        return super.getMiuiLongPressTimeoutMs();
    }
}
