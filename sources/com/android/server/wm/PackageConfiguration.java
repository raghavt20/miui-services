package com.android.server.wm;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PackageConfiguration implements Serializable {
    private static final long serialVersionUID = 202006110800L;
    final String mName;
    private final ConcurrentHashMap<String, String> mPolicyDataMap = new ConcurrentHashMap<>();
    private transient ConcurrentHashMap<String, String> mTmpPolicyDataMap;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageConfiguration(String configurationName) {
        this.mName = configurationName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConcurrentHashMap<String, String> getPolicyDataMap() {
        return this.mPolicyDataMap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCompleted() {
        this.mPolicyDataMap.clear();
        this.mPolicyDataMap.putAll(this.mTmpPolicyDataMap);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateFromScpm(String packageName, String value) {
        if (value == null) {
            value = "";
        }
        this.mTmpPolicyDataMap.put(packageName, value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePrepared() {
        ConcurrentHashMap<String, String> concurrentHashMap = this.mTmpPolicyDataMap;
        if (concurrentHashMap == null) {
            this.mTmpPolicyDataMap = new ConcurrentHashMap<>();
        } else {
            concurrentHashMap.clear();
        }
    }
}
