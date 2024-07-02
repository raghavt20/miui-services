package com.android.server.policy;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.policy.KeyCombinationManager;
import com.miui.server.input.custom.InputMiuiDesktopMode;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.input.util.ShortCutActionsUtils;

/* loaded from: classes.dex */
public class KeyboardCombinationRule extends KeyCombinationManager.TwoKeysCombinationRule {
    static final String CLASS_NAME = "className";
    static final String PACKAGE_NAME = "packageName";
    static final String TAG = "KeyboardCombinationRule";
    public Context mContext;
    private String mFunction;
    public Handler mHandler;
    public MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo mKeyboardShortcutInfo;
    private int mMetaState;
    private final int mPrimaryKey;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public KeyboardCombinationRule(Context context, Handler handler, MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        super(0, 0);
        this.mMetaState = 0;
        this.mContext = context;
        this.mHandler = handler;
        this.mKeyboardShortcutInfo = info;
        this.mPrimaryKey = getPrimaryKey();
        setFunction();
    }

    public boolean shouldInterceptKeys(int keyCode, int metaState) {
        boolean isMatch = this.mPrimaryKey == keyCode && this.mMetaState == metaState;
        if (isMatch && InputMiuiDesktopMode.shouldInterceptKeyboardCombinationRule(this.mContext, metaState, keyCode)) {
            return false;
        }
        return isMatch;
    }

    public void execute() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.KeyboardCombinationRule$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                KeyboardCombinationRule.this.lambda$execute$0();
            }
        });
    }

    void cancel() {
    }

    /* renamed from: triggerFunction, reason: merged with bridge method [inline-methods] */
    public void lambda$execute$0() {
        if (TextUtils.isEmpty(this.mFunction)) {
            Slog.i(TAG, "function is null");
            return;
        }
        Bundle extras = new Bundle();
        if ("launch_app".equals(this.mFunction)) {
            extras.putString("packageName", this.mKeyboardShortcutInfo.getPackageName());
            extras.putString("className", this.mKeyboardShortcutInfo.getClassName());
        }
        InputOneTrackUtil.getInstance(this.mContext).trackKeyboardShortcut(MiuiKeyboardUtil.KeyBoardShortcut.getShortCutNameByType(this.mKeyboardShortcutInfo));
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mFunction, this.mMetaState + "+" + this.mPrimaryKey, extras, false);
    }

    public int getPrimaryKey() {
        long keyCode = this.mKeyboardShortcutInfo.getShortcutKeyCode();
        if ((281474976710656L & keyCode) != 0) {
            keyCode &= -281474976710657L;
            this.mMetaState |= 65536;
        }
        if ((8589934592L & keyCode) != 0) {
            keyCode &= -8589934593L;
            if ((137438953472L & keyCode) != 0) {
                keyCode &= -137438953473L;
                this.mMetaState |= 34;
            } else if ((68719476736L & keyCode) != 0) {
                keyCode &= -68719476737L;
                this.mMetaState |= 18;
            }
        }
        if ((17592186044416L & keyCode) != 0) {
            keyCode &= -17592186044417L;
            this.mMetaState |= 4096;
        }
        if ((4294967296L & keyCode) != 0) {
            keyCode &= -4294967297L;
            this.mMetaState |= 1;
        }
        return (int) keyCode;
    }

    public int getMetaState() {
        return this.mMetaState;
    }

    private void setFunction() {
        switch (this.mKeyboardShortcutInfo.getType()) {
            case 0:
                this.mFunction = "launch_app";
                return;
            case 1:
                this.mFunction = "launch_control_center";
                return;
            case 2:
                this.mFunction = "launch_notification_center";
                return;
            case 3:
                this.mFunction = "launch_recents";
                return;
            case 4:
                this.mFunction = "launch_home";
                return;
            case 5:
                this.mFunction = "go_to_sleep";
                return;
            case 6:
                this.mFunction = "screen_shot";
                return;
            case 7:
                this.mFunction = "partial_screen_shot";
                return;
            case 8:
                this.mFunction = "close_app";
                return;
            case 9:
                this.mFunction = "launch_app_small_window";
                return;
            case 10:
                this.mFunction = "launch_app_full_window";
                return;
            case 11:
                this.mFunction = "launch_split_screen_to_left";
                return;
            case 12:
                this.mFunction = "launch_split_screen_to_right";
                return;
            default:
                this.mFunction = "";
                return;
        }
    }

    public String toString() {
        return "KeyboardCombinationRule{mPrimaryKey=" + this.mPrimaryKey + ", mKeyboardShortcutInfo=" + this.mKeyboardShortcutInfo + ", mMetaState=" + this.mMetaState + ", mFunction='" + this.mFunction + "'}";
    }
}
