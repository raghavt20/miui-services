package com.android.server.input.shortcut.singlekeyrule;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import com.android.server.policy.MiuiShortcutTriggerHelper;
import com.android.server.policy.MiuiSingleKeyRule;
import com.android.server.policy.SingleKeyGestureDetector;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class MiuiSingleKeyRuleManager {
    private static final String TAG = "MiuiSingleKeyRuleManager";
    private static volatile MiuiSingleKeyRuleManager sMiuiSingleKeyRuleManager;
    private final Context mContext;
    private final Handler mHandler;
    private final MiuiShortcutTriggerHelper mMiuiShortcutTriggerHelper;
    private final SingleKeyGestureDetector mSingleKeyGestureDetector;
    private final HashMap<Integer, MiuiSingleKeyRule> mSingleKeyRuleHashMap = new HashMap<>();
    private final HashMap<String, MiuiSingleKeyRule> mSingleKeyRuleHashMapForAction = new HashMap<>();

    private MiuiSingleKeyRuleManager(Handler handler, Context context, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper, SingleKeyGestureDetector singleKeyGestureDetector) {
        this.mHandler = handler;
        this.mContext = context;
        this.mMiuiShortcutTriggerHelper = miuiShortcutTriggerHelper;
        this.mSingleKeyGestureDetector = singleKeyGestureDetector;
    }

    public static MiuiSingleKeyRuleManager getInstance(Context context, Handler handler, MiuiShortcutTriggerHelper miuiShortcutTriggerHelper, SingleKeyGestureDetector singleKeyGestureDetector) {
        if (sMiuiSingleKeyRuleManager == null) {
            synchronized (MiuiSingleKeyRuleManager.class) {
                if (sMiuiSingleKeyRuleManager == null) {
                    sMiuiSingleKeyRuleManager = new MiuiSingleKeyRuleManager(handler, context, miuiShortcutTriggerHelper, singleKeyGestureDetector);
                }
            }
        }
        return sMiuiSingleKeyRuleManager;
    }

    public MiuiSingleKeyRule getMiuiSingleKeyRule(int primaryKey, MiuiSingleKeyInfo miuiSingleKeyInfo, int currentUserId) {
        MiuiSingleKeyRule miuiSingleKeyRule;
        switch (primaryKey) {
            case 25:
                miuiSingleKeyRule = new VolumeDownKeyRule(this.mContext, this.mHandler, miuiSingleKeyInfo, this.mMiuiShortcutTriggerHelper, currentUserId);
                break;
            case 26:
                miuiSingleKeyRule = new PowerKeyRule(this.mContext, this.mHandler, miuiSingleKeyInfo, this.mMiuiShortcutTriggerHelper, currentUserId);
                break;
            case 27:
                miuiSingleKeyRule = new CameraKeyRule(this.mContext, this.mHandler, miuiSingleKeyInfo, currentUserId);
                break;
            default:
                miuiSingleKeyRule = new DefaultSingleKeyRule(this.mContext, this.mHandler, miuiSingleKeyInfo, this.mMiuiShortcutTriggerHelper, currentUserId);
                break;
        }
        Slog.i(TAG, "create single key rule,primary=" + miuiSingleKeyRule.getPrimaryKey());
        this.mSingleKeyGestureDetector.removeRule(miuiSingleKeyRule);
        return miuiSingleKeyRule;
    }

    public void addRule(MiuiSingleKeyInfo miuiSingleKeyInfo, MiuiSingleKeyRule miuiSingleKeyRule) {
        int primaryKey = miuiSingleKeyInfo.getPrimaryKey();
        this.mSingleKeyRuleHashMap.put(Integer.valueOf(primaryKey), miuiSingleKeyRule);
        for (Map.Entry<String, String> actionMapEntry : miuiSingleKeyInfo.getActionAndDefaultFunctionMap().entrySet()) {
            this.mSingleKeyRuleHashMapForAction.put(actionMapEntry.getKey(), miuiSingleKeyRule);
        }
    }

    public void removeRule(int primaryKey) {
        this.mSingleKeyRuleHashMap.remove(Integer.valueOf(primaryKey));
    }

    public MiuiSingleKeyRule getSingleKeyRuleForPrimaryKey(int primaryKey) {
        return this.mSingleKeyRuleHashMap.get(Integer.valueOf(primaryKey));
    }

    public String getFunction(String action) {
        return this.mSingleKeyRuleHashMapForAction.get(action).getFunction(action);
    }

    public void onUserSwitch(final int currentUserId, final boolean isNewUser) {
        this.mSingleKeyRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyRuleManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiSingleKeyRule) obj2).onUserSwitch(currentUserId, isNewUser);
            }
        });
    }

    public void resetShortcutSettings() {
        this.mSingleKeyRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyRuleManager$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiSingleKeyRule) obj2).getObserver().setDefaultFunction(true);
            }
        });
    }

    public boolean hasActionInSingleKeyMap(String action) {
        return this.mSingleKeyRuleHashMapForAction.containsKey(action);
    }

    public void updatePolicyFlag(int policyFlags) {
        this.mSingleKeyRuleHashMap.get(25).updatePolicyFlag(policyFlags);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        for (Map.Entry<Integer, MiuiSingleKeyRule> miuiSingleKeyRuleEntry : this.mSingleKeyRuleHashMap.entrySet()) {
            pw.print(prefix2);
            pw.println(miuiSingleKeyRuleEntry.getValue());
            miuiSingleKeyRuleEntry.getValue().dump(prefix2, pw);
        }
    }

    public void initSingleKeyRule() {
        this.mSingleKeyRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.singlekeyrule.MiuiSingleKeyRuleManager$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiSingleKeyRule) obj2).init();
            }
        });
    }
}
