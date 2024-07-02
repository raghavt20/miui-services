package com.android.server.input;

import android.content.Context;
import android.os.Handler;
import android.util.LongSparseArray;
import android.view.KeyEvent;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.policy.KeyboardCombinationRule;
import com.android.server.policy.MiuiKeyInterceptExtend;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class KeyboardCombinationManagerStubImpl implements KeyboardCombinationManagerStub {
    static final String TAG = "KeyboardCombinationManagerStubImpl";
    private KeyboardCombinationRule mActiveRule;
    private Context mContext;
    private Handler mHandler;
    public LongSparseArray<KeyboardCombinationRule> mKeyboardShortcutRules = new LongSparseArray<>();
    private MiuiCustomizeShortCutUtils mMiuiCustomizeShortCutUtils;
    private MiuiKeyInterceptExtend mMiuiKeyInterceptExtend;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<KeyboardCombinationManagerStubImpl> {

        /* compiled from: KeyboardCombinationManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final KeyboardCombinationManagerStubImpl INSTANCE = new KeyboardCombinationManagerStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public KeyboardCombinationManagerStubImpl m1402provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public KeyboardCombinationManagerStubImpl m1401provideNewInstance() {
            return new KeyboardCombinationManagerStubImpl();
        }
    }

    public static KeyboardCombinationManagerStubImpl getInstance() {
        return (KeyboardCombinationManagerStubImpl) KeyboardCombinationManagerStub.get();
    }

    public void init(Context context) {
        this.mContext = context;
        this.mHandler = MiuiInputThread.getHandler();
        this.mMiuiCustomizeShortCutUtils = MiuiCustomizeShortCutUtils.getInstance(this.mContext);
        this.mMiuiKeyInterceptExtend = MiuiKeyInterceptExtend.getInstance(this.mContext);
        initRules();
    }

    public void initRules() {
        LongSparseArray<MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo> infos = this.mMiuiCustomizeShortCutUtils.getMiuiKeyboardShortcutInfo();
        for (int i = 0; i < infos.size(); i++) {
            MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info = infos.valueAt(i);
            if (info.isEnable()) {
                KeyboardCombinationRule rule = new KeyboardCombinationRule(this.mContext, this.mHandler, info);
                this.mKeyboardShortcutRules.put(info.getShortcutKeyCode(), rule);
            }
        }
    }

    public boolean interceptKey(KeyEvent event) {
        if (!this.mMiuiKeyInterceptExtend.getKeyboardShortcutEnable()) {
            this.mActiveRule = null;
            return false;
        }
        int keyCode = event.getKeyCode();
        int metaState = getMetaState(event);
        if (metaState == 0) {
            this.mActiveRule = null;
            return false;
        }
        return interceptKeyboardCombination(keyCode, metaState);
    }

    public boolean interceptKeyboardCombination(final int keyCode, final int metaState) {
        return forAllKeyboardCombinationRules(new ToBooleanFunction() { // from class: com.android.server.input.KeyboardCombinationManagerStubImpl$$ExternalSyntheticLambda1
            public final boolean apply(Object obj) {
                boolean lambda$interceptKeyboardCombination$0;
                lambda$interceptKeyboardCombination$0 = KeyboardCombinationManagerStubImpl.this.lambda$interceptKeyboardCombination$0(keyCode, metaState, (KeyboardCombinationRule) obj);
                return lambda$interceptKeyboardCombination$0;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$interceptKeyboardCombination$0(int keyCode, int metaState, KeyboardCombinationRule rule) {
        if (!rule.shouldInterceptKeys(keyCode, metaState)) {
            this.mActiveRule = null;
            return false;
        }
        this.mActiveRule = rule;
        rule.execute();
        return true;
    }

    public boolean isKeyConsumed(KeyEvent event) {
        KeyboardCombinationRule keyboardCombinationRule = this.mActiveRule;
        return keyboardCombinationRule != null && keyboardCombinationRule.shouldInterceptKeys(event.getKeyCode(), getMetaState(event));
    }

    private int getMetaState(KeyEvent event) {
        int metaState = 0;
        if (event.isCtrlPressed()) {
            metaState = 0 | 4096;
        }
        if (event.isAltPressed()) {
            if ((event.getMetaState() & 32) != 0) {
                metaState |= 34;
            } else if ((event.getMetaState() & 16) != 0) {
                metaState |= 18;
            }
        }
        if (event.isShiftPressed()) {
            metaState |= 1;
        }
        if (event.isMetaPressed()) {
            return metaState | 65536;
        }
        return metaState;
    }

    public void addRule(final MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.KeyboardCombinationManagerStubImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                KeyboardCombinationManagerStubImpl.this.lambda$addRule$1(info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$addRule$1(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mKeyboardShortcutRules.put(info.getShortcutKeyCode(), new KeyboardCombinationRule(this.mContext, this.mHandler, info));
    }

    public void removeRule(final MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.KeyboardCombinationManagerStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                KeyboardCombinationManagerStubImpl.this.lambda$removeRule$2(info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeRule$2(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mKeyboardShortcutRules.delete(info.getShortcutKeyCode());
    }

    public void updateRule(final MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.KeyboardCombinationManagerStubImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                KeyboardCombinationManagerStubImpl.this.lambda$updateRule$3(info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateRule$3(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mKeyboardShortcutRules.delete(info.getHistoryKeyCode());
        if (info.isEnable()) {
            this.mKeyboardShortcutRules.put(info.getShortcutKeyCode(), new KeyboardCombinationRule(this.mContext, this.mHandler, info));
        }
    }

    public void resetRule() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.KeyboardCombinationManagerStubImpl$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                KeyboardCombinationManagerStubImpl.this.lambda$resetRule$4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$resetRule$4() {
        this.mKeyboardShortcutRules.clear();
        initRules();
    }

    private boolean forAllKeyboardCombinationRules(ToBooleanFunction<KeyboardCombinationRule> callback) {
        int count = this.mKeyboardShortcutRules.size();
        for (int index = 0; index < count; index++) {
            KeyboardCombinationRule rule = this.mKeyboardShortcutRules.valueAt(index);
            if (callback.apply(rule)) {
                return true;
            }
        }
        return false;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.println(TAG);
        pw.print("mKeyboardShortcutRules =");
        for (int i = 0; i < this.mKeyboardShortcutRules.size(); i++) {
            pw.println(this.mKeyboardShortcutRules.valueAt(i).toString());
        }
    }
}
