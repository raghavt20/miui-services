package com.android.server.policy;

import android.content.Context;
import android.os.Handler;

/* loaded from: classes.dex */
public class MiuiGestureRule {
    private final String mAction;
    private final Context mContext;
    private int mCurrentUserId;
    private String mFunction;
    private final Handler mHandler;
    private MiuiShortcutObserver mMiuiShortcutObserver;

    public MiuiGestureRule(Context context, Handler handler, String action, String function, int currentUserId) {
        this.mContext = context;
        this.mHandler = handler;
        this.mAction = action;
        this.mFunction = function;
        this.mCurrentUserId = currentUserId;
    }

    public void init() {
        MiuiCombinationKeyAndGestureObserver miuiCombinationKeyAndGestureObserver = new MiuiCombinationKeyAndGestureObserver(this.mContext, this.mHandler, this.mAction, this.mFunction, this.mCurrentUserId);
        this.mMiuiShortcutObserver = miuiCombinationKeyAndGestureObserver;
        miuiCombinationKeyAndGestureObserver.setRuleForObserver(getInstance());
        this.mMiuiShortcutObserver.setDefaultFunction(false);
    }

    private MiuiGestureRule getInstance() {
        return this;
    }

    public MiuiShortcutObserver getObserver() {
        return this.mMiuiShortcutObserver;
    }

    public String getFunction() {
        return this.mMiuiShortcutObserver.getFunction();
    }

    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mMiuiShortcutObserver.onUserSwitch(currentUserId, isNewUser);
    }
}
