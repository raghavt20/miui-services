package com.android.server.input.shortcut.combinationkeyrule;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import com.android.server.policy.MiuiCombinationRule;
import com.miui.server.input.util.ShortCutActionsUtils;

/* loaded from: classes.dex */
public class DefaultCombinationKeyRule extends MiuiCombinationRule {
    private final String mAction;
    private final Context mContext;
    private String mFunction;
    private final Handler mHandler;

    public DefaultCombinationKeyRule(Context context, Handler handler, int primaryKey, int combinationKey, String action, String defaultFunction, int currentUserId) {
        super(context, handler, primaryKey, combinationKey, action, defaultFunction, currentUserId);
        this.mContext = context;
        this.mHandler = handler;
        this.mAction = action;
    }

    @Override // com.android.server.policy.MiuiCombinationRule
    protected void miuiExecute() {
        triggerFunction();
    }

    @Override // com.android.server.policy.MiuiCombinationRule
    protected boolean miuiPreCondition() {
        String function = getFunction();
        this.mFunction = function;
        return (TextUtils.isEmpty(function) || "none".equals(this.mFunction)) ? false : true;
    }

    private void triggerFunction() {
        Bundle bundle = null;
        if ("close_app".equals(this.mFunction)) {
            bundle = new Bundle();
            bundle.putString(ShortCutActionsUtils.EXTRA_SHORTCUT_TYPE, ShortCutActionsUtils.TYPE_PHONE_SHORTCUT);
        }
        postTriggerFunction(this.mAction, this.mFunction, bundle, true);
    }

    private void postTriggerFunction(final String action, final String function, final Bundle bundle, final boolean hapticFeedback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shortcut.combinationkeyrule.DefaultCombinationKeyRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DefaultCombinationKeyRule.this.lambda$postTriggerFunction$0(function, action, bundle, hapticFeedback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$postTriggerFunction$0(String function, String action, Bundle bundle, boolean hapticFeedback) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, action, bundle, hapticFeedback);
    }
}
