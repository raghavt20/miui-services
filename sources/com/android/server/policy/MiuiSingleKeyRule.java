package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyInfo;
import com.android.server.policy.MiuiShortcutObserver;
import com.android.server.policy.SingleKeyGestureDetector;
import java.io.PrintWriter;
import java.util.Map;

/* loaded from: classes.dex */
public abstract class MiuiSingleKeyRule extends SingleKeyGestureDetector.SingleKeyRule {
    public static final String ACTION_TYPE_DOUBLE_CLICK = "doubleClick";
    public static final String ACTION_TYPE_FIVE_CLICK = "fiveClick";
    public static final String ACTION_TYPE_LONG_PRESS = "longPress";
    public static final long DEFAULT_VERY_LONG_PRESS_TIME_OUT = 3000;
    public static final int DOUBLE_TIMES_TAP = 2;
    public static final int FIVE_TIMES_TAP = 5;
    public static final int SINGLE_TAP = 1;
    private static final String TAG = "MiuiSingleKeyRule";
    private final Context mContext;
    private int mCurrentUserId;
    private final Handler mHandler;
    private final int mKeyCode;
    private MiuiShortcutObserver mMiuiShortcutObserver;
    private MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private final MiuiSingleKeyInfo mMiuiSingleKeyInfo;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public MiuiSingleKeyRule(Context context, Handler handler, MiuiSingleKeyInfo miuiSingleKeyInfo, int currentUserId) {
        super(miuiSingleKeyInfo.getPrimaryKey());
        this.mKeyCode = miuiSingleKeyInfo.getPrimaryKey();
        this.mContext = context;
        this.mHandler = handler;
        this.mCurrentUserId = currentUserId;
        this.mMiuiSingleKeyInfo = miuiSingleKeyInfo;
    }

    public void init() {
        this.mMiuiShortcutTriggerHelper = MiuiShortcutTriggerHelper.getInstance(this.mContext);
        MiuiSingleKeyObserver miuiSingleKeyObserver = new MiuiSingleKeyObserver(this.mHandler, this.mContext, this.mMiuiSingleKeyInfo.getActionAndDefaultFunctionMap(), this.mCurrentUserId);
        this.mMiuiShortcutObserver = miuiSingleKeyObserver;
        miuiSingleKeyObserver.setRuleForObserver(getInstance());
        this.mMiuiShortcutObserver.setDefaultFunction(false);
    }

    public void registerShortcutListener(MiuiShortcutObserver.MiuiShortcutListener miuiShortcutListener) {
        this.mMiuiShortcutObserver.registerShortcutListener(miuiShortcutListener);
    }

    private MiuiSingleKeyRule getInstance() {
        return this;
    }

    public MiuiShortcutObserver getObserver() {
        return this.mMiuiShortcutObserver;
    }

    public int getPrimaryKey() {
        return this.mKeyCode;
    }

    public String getFunction(String action) {
        return this.mMiuiShortcutObserver.getFunction(action);
    }

    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mMiuiShortcutObserver.onUserSwitch(currentUserId, isNewUser);
    }

    public Map<String, String> getActionAndFunctionMap() {
        return this.mMiuiShortcutObserver.getActionAndFunctionMap();
    }

    public void updatePolicyFlag(int policyFlags) {
    }

    public void dump(String prefix, PrintWriter pw) {
        this.mMiuiShortcutObserver.dump(prefix, pw);
    }

    boolean supportLongPress() {
        return miuiSupportLongPress();
    }

    protected boolean miuiSupportLongPress() {
        return true;
    }

    boolean supportVeryLongPress() {
        return miuiSupportVeryLongPress();
    }

    protected boolean miuiSupportVeryLongPress() {
        return false;
    }

    void onPress(long downTime) {
        onMiuiPress(downTime);
    }

    void onKeyDown(KeyEvent event) {
        onMiuiKeyDown(event);
    }

    protected void onMiuiKeyDown(KeyEvent event) {
    }

    protected void onMiuiPress(long downTime) {
    }

    void onMultiPress(long downTime, int count) {
        onMiuiMultiPress(downTime, count);
    }

    protected void onMiuiMultiPress(long downTime, int count) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLongPressTimeoutMs() {
        return getMiuiLongPressTimeoutMs();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public long getMiuiLongPressTimeoutMs() {
        return this.mMiuiShortcutTriggerHelper.getDefaultLongPressTimeOut();
    }

    void onLongPress(long eventTime) {
        this.mMiuiShortcutTriggerHelper.notifyLongPressed(this.mKeyCode);
        onMiuiLongPress(eventTime);
    }

    protected void onMiuiLongPress(long eventTime) {
    }

    long getVeryLongPressTimeoutMs() {
        return getMiuiVeryLongPressTimeoutMs();
    }

    protected long getMiuiVeryLongPressTimeoutMs() {
        return 3000L;
    }

    void onVeryLongPress(long eventTime) {
        onMiuiVeryLongPress(eventTime);
    }

    protected void onMiuiVeryLongPress(long eventTime) {
    }

    void onLongPressKeyUp(KeyEvent event) {
        onMiuiLongPressKeyUp(event);
    }

    protected void onMiuiLongPressKeyUp(KeyEvent event) {
    }

    int getMaxMultiPressCount() {
        return getMiuiMaxMultiPressCount();
    }

    protected int getMiuiMaxMultiPressCount() {
        return 1;
    }

    public String toString() {
        return "KeyCode=" + this.mKeyCode + ", SupportLongPress=" + miuiSupportLongPress() + ", SupportVeryLongPress=" + miuiSupportVeryLongPress() + ", LongPressTimeOut=" + getLongPressTimeoutMs() + ", VeryLongPressTimeOut=" + getVeryLongPressTimeoutMs() + ", MaxCount=" + getMaxMultiPressCount();
    }
}
