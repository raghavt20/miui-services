package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;

/* loaded from: classes.dex */
public class MiuiCombinationKeyAndGestureObserver extends MiuiShortcutObserver {
    private static final String TAG = "MiuiCombinationKeyAndGestureObserver";
    private final String mAction;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final String mDefaultFunction;
    private String mFunction;
    private MiuiCombinationRule mMiuiCombinationRule;
    private MiuiGestureRule mMiuiGestureRule;

    public MiuiCombinationKeyAndGestureObserver(Context context, Handler handler, String action, String defaultFunction, int currentUserId) {
        super(handler, context, currentUserId);
        this.mAction = action;
        this.mDefaultFunction = defaultFunction;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        registerShortcutAction(action);
    }

    private void registerShortcutAction(String action) {
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor(action), false, this, -1);
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange) {
        updateFunction();
        MiuiGestureRule miuiGestureRule = this.mMiuiGestureRule;
        if (miuiGestureRule != null) {
            notifyGestureRuleChanged(miuiGestureRule);
        }
        MiuiCombinationRule miuiCombinationRule = this.mMiuiCombinationRule;
        if (miuiCombinationRule != null) {
            notifyCombinationRuleChanged(miuiCombinationRule);
        }
        super.onChange(selfChange);
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void setDefaultFunction(boolean isNeedReset) {
        String currentFunction;
        if (currentFunctionValueIsInt(this.mAction)) {
            int functionStatus = Settings.System.getIntForUser(this.mContentResolver, this.mAction, -1, this.mCurrentUserId);
            currentFunction = functionStatus == -1 ? null : String.valueOf(functionStatus);
        } else {
            String currentFunction2 = Settings.System.getStringForUser(this.mContentResolver, this.mAction, this.mCurrentUserId);
            currentFunction = currentFunction2 == null ? this.mDefaultFunction : currentFunction2;
        }
        if (isNeedReset) {
            currentFunction = this.mDefaultFunction;
        }
        if (!hasCustomizedFunction(this.mAction, currentFunction, isNeedReset)) {
            if (isFeasibleFunction(currentFunction, this.mContext)) {
                Settings.System.putStringForUser(this.mContentResolver, this.mAction, currentFunction, this.mCurrentUserId);
            } else if (isNeedReset) {
                Settings.System.putStringForUser(this.mContentResolver, this.mAction, null, this.mCurrentUserId);
            }
        }
        super.setDefaultFunction(isNeedReset);
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    void updateRuleInfo() {
        onChange(false);
    }

    private void updateFunction() {
        String function;
        if (currentFunctionValueIsInt(this.mAction)) {
            int functionStatus = Settings.System.getIntForUser(this.mContentResolver, this.mAction, -1, this.mCurrentUserId);
            function = functionStatus == -1 ? null : String.valueOf(functionStatus);
        } else {
            function = Settings.System.getStringForUser(this.mContentResolver, this.mAction, this.mCurrentUserId);
        }
        this.mFunction = function;
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void setRuleForObserver(MiuiCombinationRule miuiCombinationRule) {
        this.mMiuiCombinationRule = miuiCombinationRule;
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void setRuleForObserver(MiuiGestureRule miuiGestureRule) {
        this.mMiuiGestureRule = miuiGestureRule;
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public String getFunction() {
        return this.mFunction;
    }
}
