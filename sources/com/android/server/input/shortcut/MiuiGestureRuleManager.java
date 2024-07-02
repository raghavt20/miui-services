package com.android.server.input.shortcut;

import android.content.Context;
import android.os.Handler;
import com.android.server.policy.MiuiGestureRule;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class MiuiGestureRuleManager {
    private static final String TAG = "MiuiGestureRuleManager";
    private static volatile MiuiGestureRuleManager sGestureManager;
    private final Context mContext;
    private final HashMap<String, MiuiGestureRule> mGestureRuleHashMap = new HashMap<>();
    private final Handler mHandler;

    private MiuiGestureRuleManager(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public static MiuiGestureRuleManager getInstance(Context context, Handler handler) {
        if (sGestureManager == null) {
            synchronized (MiuiGestureRuleManager.class) {
                if (sGestureManager == null) {
                    sGestureManager = new MiuiGestureRuleManager(context, handler);
                }
            }
        }
        return sGestureManager;
    }

    public void addRule(String action, MiuiGestureRule miuiGestureRule) {
        this.mGestureRuleHashMap.put(action, miuiGestureRule);
    }

    public void removeRule(String action) {
        this.mGestureRuleHashMap.remove(action);
    }

    public String getFunction(String action) {
        return this.mGestureRuleHashMap.get(action).getFunction();
    }

    public MiuiGestureRule getGestureManager(String action) {
        return this.mGestureRuleHashMap.get(action);
    }

    public boolean hasActionInGestureRuleMap(String action) {
        return this.mGestureRuleHashMap.containsKey(action);
    }

    public void onUserSwitch(final int currentUserId, final boolean isNewUser) {
        this.mGestureRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.MiuiGestureRuleManager$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiGestureRule) obj2).onUserSwitch(currentUserId, isNewUser);
            }
        });
    }

    public void resetDefaultFunction() {
        this.mGestureRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.MiuiGestureRuleManager$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiGestureRule) obj2).getObserver().setDefaultFunction(true);
            }
        });
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        for (Map.Entry<String, MiuiGestureRule> miuiGestureRuleEntry : this.mGestureRuleHashMap.entrySet()) {
            pw.print(prefix2);
            pw.print(miuiGestureRuleEntry.getKey());
            pw.print("=");
            pw.println(miuiGestureRuleEntry.getValue().getFunction());
        }
    }

    public void initGestureRule() {
        this.mGestureRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.input.shortcut.MiuiGestureRuleManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((MiuiGestureRule) obj2).init();
            }
        });
    }
}
