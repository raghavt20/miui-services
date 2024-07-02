package com.android.server.input.shortcut.combinationkeyrule;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import com.android.server.policy.KeyCombinationManager;
import com.android.server.policy.MiuiCombinationRule;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class MiuiCombinationRuleManager {
    private static final String TAG = "MiuiCombinationRuleManager";
    private static volatile MiuiCombinationRuleManager sMiuiCombinationRuleManager;
    private final HashMap<String, MiuiCombinationRule> mCombinationRuleHashMap = new HashMap<>();
    private final Context mContext;
    private final Handler mHandler;
    private final KeyCombinationManager mKeyCombinationManager;

    private MiuiCombinationRuleManager(Context context, Handler handler, KeyCombinationManager keyCombinationManager) {
        this.mContext = context;
        this.mHandler = handler;
        this.mKeyCombinationManager = keyCombinationManager;
    }

    public static MiuiCombinationRuleManager getInstance(Context context, Handler handler, KeyCombinationManager keyCombinationManager) {
        if (sMiuiCombinationRuleManager == null) {
            synchronized (MiuiCombinationRuleManager.class) {
                if (sMiuiCombinationRuleManager == null) {
                    sMiuiCombinationRuleManager = new MiuiCombinationRuleManager(context, handler, keyCombinationManager);
                }
            }
        }
        return sMiuiCombinationRuleManager;
    }

    public MiuiCombinationRule getMiuiCombinationRule(int primaryKey, int combinationKey, String action, String function, int currentUserId) {
        MiuiCombinationRule miuiCombinationRule = new DefaultCombinationKeyRule(this.mContext, this.mHandler, primaryKey, combinationKey, action, function, currentUserId);
        if (!shouldHoldOnAOSPLogic(miuiCombinationRule)) {
            this.mKeyCombinationManager.removeRule(miuiCombinationRule);
        }
        Slog.d(TAG, "create miui combination rule,primaryKey=" + primaryKey + " combinationKey=" + combinationKey);
        return miuiCombinationRule;
    }

    public boolean shouldHoldOnAOSPLogic(MiuiCombinationRule miuiCombinationRule) {
        return (26 == miuiCombinationRule.getPrimaryKey() && 25 == miuiCombinationRule.getCombinationKey()) || (25 == miuiCombinationRule.getPrimaryKey() && 26 == miuiCombinationRule.getCombinationKey());
    }

    public void addRule(String action, MiuiCombinationRule miuiCombinationRule) {
        this.mCombinationRuleHashMap.put(action, miuiCombinationRule);
    }

    public void removeRule(String action) {
        this.mCombinationRuleHashMap.remove(action);
    }

    public void onUserSwitch(final int currentUserId, final boolean isNewUser) {
        this.mCombinationRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.combinationkeyrule.MiuiCombinationRuleManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiCombinationRule) obj2).onUserSwitch(currentUserId, isNewUser);
            }
        });
    }

    public void resetDefaultFunction() {
        this.mCombinationRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.combinationkeyrule.MiuiCombinationRuleManager$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiCombinationRule) obj2).getObserver().setDefaultFunction(true);
            }
        });
    }

    public MiuiCombinationRule getCombinationRule(String action) {
        return this.mCombinationRuleHashMap.get(action);
    }

    public boolean hasActionInCombinationKeyMap(String action) {
        return this.mCombinationRuleHashMap.containsKey(action);
    }

    public String getFunction(String action) {
        return this.mCombinationRuleHashMap.get(action).getFunction();
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        for (Map.Entry<String, MiuiCombinationRule> miuiCombinationRuleEntry : this.mCombinationRuleHashMap.entrySet()) {
            pw.print(prefix2);
            pw.println(miuiCombinationRuleEntry.getValue());
        }
    }

    public void initCombinationKeyRule() {
        this.mCombinationRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.combinationkeyrule.MiuiCombinationRuleManager$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiCombinationRule) obj2).init();
            }
        });
    }
}
