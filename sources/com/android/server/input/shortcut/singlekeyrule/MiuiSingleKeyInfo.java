package com.android.server.input.shortcut.singlekeyrule;

import java.util.Map;

/* loaded from: classes.dex */
public class MiuiSingleKeyInfo {
    private final Map<String, String> mActionAndDefaultFunctionMap;
    private final Map<String, String> mActionMapForType;
    private final Map<String, Integer> mActionMaxCountMap;
    private final long mLongPressTimeOut;
    private final int mPrimaryKey;

    public MiuiSingleKeyInfo(int primaryKey, Map<String, String> actionAndDefaultFunctionMap, long longPressTimeOut, Map<String, Integer> actionMaxCountMap, Map<String, String> actionMapForType) {
        this.mPrimaryKey = primaryKey;
        this.mActionAndDefaultFunctionMap = actionAndDefaultFunctionMap;
        this.mLongPressTimeOut = longPressTimeOut;
        this.mActionMaxCountMap = actionMaxCountMap;
        this.mActionMapForType = actionMapForType;
    }

    public Map<String, String> getActionAndDefaultFunctionMap() {
        return this.mActionAndDefaultFunctionMap;
    }

    public long getLongPressTimeOut() {
        return this.mLongPressTimeOut;
    }

    public int getPrimaryKey() {
        return this.mPrimaryKey;
    }

    public Map<String, Integer> getActionMaxCountMap() {
        return this.mActionMaxCountMap;
    }

    public Map<String, String> getActionMapForType() {
        return this.mActionMapForType;
    }
}
