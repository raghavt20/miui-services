package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import com.android.server.policy.KeyCombinationManager;

/* loaded from: classes.dex */
public abstract class MiuiCombinationRule extends KeyCombinationManager.TwoKeysCombinationRule {
    private final String mAction;
    private final int mCombinationKey;
    private final Context mContext;
    private int mCurrentUserId;
    private final String mDefaultFunction;
    private final Handler mHandler;
    private MiuiShortcutObserver mMiuiShortcutObserver;
    private final int mPrimaryKey;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public MiuiCombinationRule(Context context, Handler handler, int primaryKey, int combinationKey, String action, String defaultFunction, int currentUserId) {
        super(primaryKey, combinationKey);
        this.mContext = context;
        this.mHandler = handler;
        this.mAction = action;
        this.mPrimaryKey = primaryKey;
        this.mCombinationKey = combinationKey;
        this.mDefaultFunction = defaultFunction;
        this.mCurrentUserId = currentUserId;
    }

    public void init() {
        MiuiCombinationKeyAndGestureObserver miuiCombinationKeyAndGestureObserver = new MiuiCombinationKeyAndGestureObserver(this.mContext, this.mHandler, this.mAction, this.mDefaultFunction, this.mCurrentUserId);
        this.mMiuiShortcutObserver = miuiCombinationKeyAndGestureObserver;
        miuiCombinationKeyAndGestureObserver.setRuleForObserver(getInstance());
        this.mMiuiShortcutObserver.setDefaultFunction(false);
    }

    private MiuiCombinationRule getInstance() {
        return this;
    }

    public int getPrimaryKey() {
        return this.mPrimaryKey;
    }

    public int getCombinationKey() {
        return this.mCombinationKey;
    }

    public MiuiShortcutObserver getObserver() {
        return this.mMiuiShortcutObserver;
    }

    public String getAction() {
        return this.mAction;
    }

    public String getFunction() {
        return this.mMiuiShortcutObserver.getFunction();
    }

    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mMiuiShortcutObserver.onUserSwitch(currentUserId, isNewUser);
    }

    void execute() {
        miuiExecute();
    }

    protected void miuiExecute() {
    }

    void cancel() {
        cancelMiui();
    }

    protected void cancelMiui() {
    }

    public boolean preCondition() {
        return miuiPreCondition();
    }

    protected boolean miuiPreCondition() {
        return false;
    }

    public String toString() {
        return KeyEvent.keyCodeToString(this.mPrimaryKey) + " + " + KeyEvent.keyCodeToString(this.mCombinationKey) + " preCondition=" + miuiPreCondition() + " action=" + this.mAction + " function=" + getFunction();
    }
}
