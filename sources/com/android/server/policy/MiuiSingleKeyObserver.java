package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class MiuiSingleKeyObserver extends MiuiShortcutObserver {
    private static final String TAG = "MiuiSingKeyShortcutObserver";
    private Map<String, String> mActionAndDefaultFunction;
    private final Map<String, String> mActionAndFunctionMap;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private MiuiSingleKeyRule mMiuiSingleKeyRule;

    public MiuiSingleKeyObserver(Handler handler, Context context, Map<String, String> actionAndDefaultFunctionMap, int currentUserId) {
        super(handler, context, currentUserId);
        this.mActionAndFunctionMap = new HashMap();
        this.mActionAndDefaultFunction = new HashMap();
        this.mActionAndDefaultFunction = actionAndDefaultFunctionMap;
        actionAndDefaultFunctionMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiSingleKeyObserver$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiSingleKeyObserver.this.lambda$new$0((String) obj, (String) obj2);
            }
        });
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        registerShortcutAction();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(String action, String function) {
        this.mActionAndFunctionMap.put(action, null);
    }

    private void registerShortcutAction() {
        this.mActionAndFunctionMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiSingleKeyObserver$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiSingleKeyObserver.this.lambda$registerShortcutAction$1((String) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerShortcutAction$1(String action, String function) {
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor(action), false, this, -1);
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange, final Uri uri) {
        this.mActionAndFunctionMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiSingleKeyObserver$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiSingleKeyObserver.this.lambda$onChange$2(uri, (String) obj, (String) obj2);
            }
        });
        MiuiSingleKeyRule miuiSingleKeyRule = this.mMiuiSingleKeyRule;
        if (miuiSingleKeyRule != null) {
            notifySingleRuleChanged(miuiSingleKeyRule, uri);
        }
        super.onChange(selfChange, uri);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onChange$2(Uri uri, String action, String function) {
        if (Settings.System.getUriFor(action).equals(uri)) {
            updateFunction(action);
        }
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void setDefaultFunction(boolean isNeedReset) {
        String currentFunction;
        for (Map.Entry<String, String> actionAndFunctionEntry : this.mActionAndFunctionMap.entrySet()) {
            String action = actionAndFunctionEntry.getKey();
            String defaultFunction = this.mActionAndDefaultFunction.get(action);
            if (currentFunctionValueIsInt(action)) {
                int functionStatus = Settings.System.getIntForUser(this.mContentResolver, action, -1, this.mCurrentUserId);
                currentFunction = functionStatus == -1 ? null : String.valueOf(functionStatus);
            } else {
                String currentFunction2 = Settings.System.getStringForUser(this.mContentResolver, action, this.mCurrentUserId);
                currentFunction = currentFunction2 == null ? defaultFunction : currentFunction2;
            }
            if (isNeedReset) {
                currentFunction = defaultFunction;
            }
            if (!hasCustomizedFunction(action, currentFunction, isNeedReset)) {
                if (isFeasibleFunction(currentFunction, this.mContext)) {
                    Settings.System.putStringForUser(this.mContentResolver, action, currentFunction, this.mCurrentUserId);
                } else if (isNeedReset) {
                    Settings.System.putStringForUser(this.mContentResolver, action, null, this.mCurrentUserId);
                }
            }
        }
        super.setDefaultFunction(isNeedReset);
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    void updateRuleInfo() {
        this.mActionAndFunctionMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiSingleKeyObserver$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MiuiSingleKeyObserver.this.lambda$updateRuleInfo$3((String) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateRuleInfo$3(String action, String function) {
        onChange(false, Settings.System.getUriFor(action));
    }

    private void updateFunction(String action) {
        String function;
        if (currentFunctionValueIsInt(action)) {
            int functionStatus = Settings.System.getIntForUser(this.mContentResolver, action, -1, this.mCurrentUserId);
            function = String.valueOf(functionStatus);
        } else {
            function = Settings.System.getStringForUser(this.mContentResolver, action, this.mCurrentUserId);
        }
        this.mActionAndFunctionMap.put(action, function);
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public Map<String, String> getActionAndFunctionMap() {
        return this.mActionAndFunctionMap;
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public String getFunction(String action) {
        return this.mActionAndFunctionMap.get(action);
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void setRuleForObserver(MiuiSingleKeyRule miuiSingleKeyRule) {
        this.mMiuiSingleKeyRule = miuiSingleKeyRule;
    }

    @Override // com.android.server.policy.MiuiShortcutObserver
    public void dump(String prefix, PrintWriter pw) {
        String prefix2 = prefix + "    ";
        for (Map.Entry<String, String> actionAndFunctionEntry : this.mActionAndFunctionMap.entrySet()) {
            pw.print(prefix2);
            pw.print(actionAndFunctionEntry.getKey());
            pw.print("=");
            pw.println(actionAndFunctionEntry.getValue());
        }
    }
}
