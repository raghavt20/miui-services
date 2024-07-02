package com.miui.server.security.model;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.SparseArray;
import java.io.PrintWriter;
import miui.security.AppBehavior;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class PackageBehaviorBean {
    private final SparseArray<BehaviorData> mBehaviors = new SparseArray<>();

    public boolean recordAppBehavior(int behavior, String stringValue, long longValue) {
        if (!this.mBehaviors.contains(behavior)) {
            BehaviorData behaviorData = new BehaviorData();
            behaviorData.init();
            this.mBehaviors.put(behavior, behaviorData);
        }
        if (!TextUtils.isEmpty(stringValue)) {
            return this.mBehaviors.get(behavior).appendList(stringValue);
        }
        return this.mBehaviors.get(behavior).appendData(longValue);
    }

    public boolean isEmpty() {
        return this.mBehaviors.size() == 0;
    }

    public JSONObject toJson() throws JSONException {
        if (isEmpty()) {
            return null;
        }
        JSONObject packageWithBehaviors = new JSONObject();
        for (int i = 0; i < this.mBehaviors.size(); i++) {
            int recordBehavior = this.mBehaviors.keyAt(i);
            BehaviorData behaviorData = this.mBehaviors.valueAt(i);
            if (behaviorData != null) {
                if (behaviorData.stringIntegerArrayMap != null && behaviorData.stringIntegerArrayMap.size() > 0) {
                    String[] spliceData = new String[behaviorData.stringIntegerArrayMap.size()];
                    for (int k = 0; k < behaviorData.stringIntegerArrayMap.size(); k++) {
                        spliceData[k] = ((String) behaviorData.stringIntegerArrayMap.keyAt(k)) + "@" + behaviorData.stringIntegerArrayMap.valueAt(k);
                    }
                    JSONArray jsonArray = new JSONArray(spliceData);
                    packageWithBehaviors.put(String.valueOf(recordBehavior), jsonArray);
                }
                if (behaviorData.data > 0) {
                    packageWithBehaviors.put(String.valueOf(recordBehavior), behaviorData.data);
                }
            }
        }
        return packageWithBehaviors;
    }

    public int init() {
        int clearSize = 0;
        if (isEmpty()) {
            return 0;
        }
        for (int i = 0; i < this.mBehaviors.size(); i++) {
            BehaviorData behaviorData = this.mBehaviors.valueAt(i);
            if (behaviorData != null) {
                clearSize += behaviorData.init();
            }
        }
        return clearSize;
    }

    public void dump(PrintWriter pw) {
        for (int i = 0; i < this.mBehaviors.size(); i++) {
            int behavior = this.mBehaviors.keyAt(i);
            pw.println("        behavior:" + AppBehavior.behaviorToName(behavior));
            this.mBehaviors.get(behavior).dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class BehaviorData {
        private long data;
        private ArrayMap<String, Integer> stringIntegerArrayMap;

        private BehaviorData() {
        }

        public int init() {
            int clearSize = 0;
            ArrayMap<String, Integer> arrayMap = this.stringIntegerArrayMap;
            if (arrayMap != null && arrayMap.size() > 0) {
                clearSize = this.stringIntegerArrayMap.size();
                this.stringIntegerArrayMap.clear();
            }
            if (this.data != -1) {
                int clearSize2 = clearSize + 1;
                this.data = -1L;
                return clearSize2;
            }
            return clearSize;
        }

        public boolean appendList(String val) {
            if (TextUtils.isEmpty(val)) {
                return false;
            }
            if (this.stringIntegerArrayMap == null) {
                this.stringIntegerArrayMap = new ArrayMap<>();
            }
            if (this.stringIntegerArrayMap.containsKey(val)) {
                int curCount = this.stringIntegerArrayMap.getOrDefault(val, 1).intValue();
                this.stringIntegerArrayMap.put(val, Integer.valueOf(curCount + 1));
                return false;
            }
            this.stringIntegerArrayMap.put(val, 1);
            return true;
        }

        public boolean appendData(long val) {
            if (val <= 0) {
                return false;
            }
            boolean addCount = false;
            if (this.data == -1) {
                this.data = 0L;
                addCount = true;
            }
            this.data += val;
            return addCount;
        }

        public void dump(PrintWriter pw) {
            ArrayMap<String, Integer> arrayMap = this.stringIntegerArrayMap;
            if (arrayMap != null && arrayMap.size() > 0) {
                for (String tmpString : this.stringIntegerArrayMap.keySet()) {
                    pw.println("            value:" + tmpString);
                    pw.println("                count:" + this.stringIntegerArrayMap.get(tmpString));
                }
            }
            if (this.data != -1) {
                pw.println("            data:" + this.data);
            }
        }
    }
}
