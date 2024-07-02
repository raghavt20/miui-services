package com.android.server.input.padkeyboard;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class AngleStateController {
    private static final String TAG = "AngleStateController";
    private Context mContext;
    private AngleState mCurrentState;
    private PowerManager mPowerManager;
    private volatile boolean mShouldIgnoreKeyboard = false;
    private volatile boolean mLidOpen = true;
    private volatile int mKbLevel = 2;
    private volatile boolean mTabletOpen = true;
    private volatile boolean mIdentityPass = true;

    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX WARN: Unknown enum class pattern. Please report as an issue! */
    /* loaded from: classes.dex */
    public static abstract class AngleState {
        protected float lower;
        protected OnChangeListener onChangeListener;
        protected float upper;
        public static final AngleState CLOSE_STATE = new AnonymousClass1("CLOSE_STATE", 0, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 5.0f);
        public static final AngleState NO_WORK_STATE_1 = new AnonymousClass2("NO_WORK_STATE_1", 1, 5.0f, 45.0f);
        public static final AngleState WORK_STATE_1 = new AnonymousClass3("WORK_STATE_1", 2, 45.0f, 90.0f);
        public static final AngleState WORK_STATE_2 = new AnonymousClass4("WORK_STATE_2", 3, 90.0f, 185.0f);
        public static final AngleState NO_WORK_STATE_2 = new AnonymousClass5("NO_WORK_STATE_2", 4, 185.0f, 355.0f);
        public static final AngleState BACK_STATE = new AnonymousClass6("BACK_STATE", 5, 355.0f, 360.0f);
        private static final /* synthetic */ AngleState[] $VALUES = $values();

        /* loaded from: classes.dex */
        public interface OnChangeListener {
            void onChange(boolean z);
        }

        public abstract boolean getCurrentKeyboardStatus();

        public abstract boolean isCurrentState(float f);

        public abstract AngleState toNextState(float f);

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$1, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass1 extends AngleState {
            private AnonymousClass1(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper && angle <= 360.0f - this.upper) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_1.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return true;
            }
        }

        private static /* synthetic */ AngleState[] $values() {
            return new AngleState[]{CLOSE_STATE, NO_WORK_STATE_1, WORK_STATE_1, WORK_STATE_2, NO_WORK_STATE_2, BACK_STATE};
        }

        public static AngleState valueOf(String name) {
            return (AngleState) Enum.valueOf(AngleState.class, name);
        }

        public static AngleState[] values() {
            return (AngleState[]) $VALUES.clone();
        }

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$2, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass2 extends AngleState {
            private AnonymousClass2(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper) {
                    onChange(angle >= this.upper);
                    return WORK_STATE_1.toNextState(angle);
                }
                if (angle < this.lower) {
                    onChange(angle >= this.upper);
                    return CLOSE_STATE.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return true;
            }
        }

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$3, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass3 extends AngleState {
            private AnonymousClass3(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper) {
                    onChange(angle >= this.upper);
                    return WORK_STATE_2.toNextState(angle);
                }
                if (angle < this.lower) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_1.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return false;
            }
        }

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$4, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass4 extends AngleState {
            private AnonymousClass4(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_2.toNextState(angle);
                }
                if (angle < this.lower) {
                    onChange(angle >= this.upper);
                    return WORK_STATE_1.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return false;
            }
        }

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$5, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass5 extends AngleState {
            private AnonymousClass5(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper) {
                    onChange(angle >= this.upper);
                    return BACK_STATE.toNextState(angle);
                }
                if (angle < this.lower) {
                    onChange(angle >= this.upper);
                    return WORK_STATE_2.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return true;
            }
        }

        /* renamed from: com.android.server.input.padkeyboard.AngleStateController$AngleState$6, reason: invalid class name */
        /* loaded from: classes.dex */
        enum AnonymousClass6 extends AngleState {
            private AnonymousClass6(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle <= this.upper;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle < this.lower && angle > 360.0f - this.lower) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_2.toNextState(angle);
                }
                return this;
            }

            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState
            public boolean getCurrentKeyboardStatus() {
                return true;
            }
        }

        private AngleState(String str, int i, float lower, float upper) {
            this.upper = upper;
            this.lower = lower;
        }

        public void onChange(boolean toUpper) {
            OnChangeListener onChangeListener = this.onChangeListener;
            if (onChangeListener != null) {
                onChangeListener.onChange(toUpper);
            }
        }

        public void setOnChangeListener(OnChangeListener onChangeListener) {
            this.onChangeListener = onChangeListener;
        }
    }

    public AngleStateController(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        initState();
    }

    public void updateAngleState(float angle) {
        if (Float.isNaN(angle)) {
            return;
        }
        if (!this.mLidOpen) {
            angle = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        if (!this.mTabletOpen) {
            angle = 360.0f;
        }
        if (this.mCurrentState == null) {
            initCurrentState();
        }
        if (!this.mCurrentState.isCurrentState(angle)) {
            this.mCurrentState = this.mCurrentState.toNextState(angle);
        } else {
            this.mShouldIgnoreKeyboard = this.mCurrentState.getCurrentKeyboardStatus();
        }
    }

    private void initCurrentState() {
        this.mCurrentState = AngleState.CLOSE_STATE;
    }

    private void initState() {
        AngleState.CLOSE_STATE.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda0
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$0(z);
            }
        });
        AngleState.NO_WORK_STATE_1.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda1
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$1(z);
            }
        });
        AngleState.WORK_STATE_1.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda2
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$2(z);
            }
        });
        AngleState.WORK_STATE_2.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda3
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$3(z);
            }
        });
        AngleState.NO_WORK_STATE_2.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda4
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$4(z);
            }
        });
        AngleState.BACK_STATE.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.input.padkeyboard.AngleStateController$$ExternalSyntheticLambda5
            @Override // com.android.server.input.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.lambda$initState$5(z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$0(boolean toUpper) {
        Slog.i(TAG, "AngleState CLOSE_STATE onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$1(boolean toUpper) {
        Slog.i(TAG, "AngleState NO_WORK_STATE_1 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = !toUpper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$2(boolean toUpper) {
        Slog.i(TAG, "AngleState WORK_STATE_1 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = !toUpper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$3(boolean toUpper) {
        Slog.i(TAG, "AngleState WORK_STATE_2 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = toUpper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$4(boolean toUpper) {
        Slog.i(TAG, "AngleState NO_WORK_STATE_2 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = toUpper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initState$5(boolean toUpper) {
        Slog.i(TAG, "AngleState BACK_STATE onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = true;
    }

    public boolean shouldIgnoreKeyboard() {
        if (!this.mIdentityPass) {
            return true;
        }
        if (this.mKbLevel == 2) {
            return false;
        }
        return !this.mLidOpen || this.mShouldIgnoreKeyboard;
    }

    public boolean shouldIgnoreKeyboardForIIC() {
        return (this.mIdentityPass && this.mLidOpen && this.mTabletOpen && !this.mShouldIgnoreKeyboard) ? false : true;
    }

    public boolean isWorkState() {
        return !this.mShouldIgnoreKeyboard;
    }

    public void setShouldIgnoreKeyboardFromKeyboard(boolean isEnable) {
        this.mShouldIgnoreKeyboard = isEnable;
    }

    public boolean getLidStatus() {
        return this.mLidOpen;
    }

    public boolean getTabletStatus() {
        return this.mTabletOpen;
    }

    public boolean getIdentityStatus() {
        return this.mIdentityPass;
    }

    public void setKbLevel(int kbLevel, boolean shouldWakeUp) {
        this.mKbLevel = kbLevel;
        if (shouldWakeUp && this.mKbLevel == 2 && this.mLidOpen && this.mTabletOpen) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "usb_keyboard_attach");
        }
    }

    public void wakeUpIfNeed(boolean shouldWakeUp) {
        if (MiuiIICKeyboardManager.supportPadKeyboard() && shouldWakeUp && this.mLidOpen && this.mTabletOpen) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "miui_keyboard_attach");
        }
    }

    public void notifyLidSwitchChanged(boolean lidOpen) {
        this.mLidOpen = lidOpen;
        Slog.i(TAG, "notifyLidSwitchChanged: " + lidOpen);
    }

    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        this.mTabletOpen = tabletOpen;
        Slog.i(TAG, "notifyTabletSwitchChanged: " + tabletOpen);
    }

    public void setIdentityState(boolean identityPass) {
        Slog.i(TAG, "Keyboard authentication status: " + identityPass);
        this.mIdentityPass = identityPass;
    }

    public void resetAngleStatus() {
        this.mShouldIgnoreKeyboard = false;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mCurrentState=");
        pw.println(this.mCurrentState);
        pw.print(prefix);
        pw.print("mIdentityPass=");
        pw.println(this.mIdentityPass);
        pw.print(prefix);
        pw.print("mLidOpen=");
        pw.println(this.mLidOpen);
        pw.print(prefix);
        pw.print("mTabletOpen=");
        pw.println(this.mTabletOpen);
        pw.print(prefix);
        pw.print("mShouldIgnoreKeyboard=");
        pw.println(this.mShouldIgnoreKeyboard);
    }
}
