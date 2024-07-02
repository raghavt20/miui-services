package com.android.server.appcacheopt;

import java.util.Arrays;

/* loaded from: classes.dex */
public class AppCacheOptimizerConfig {
    private int mIsOptimizeEnable;
    private String[] mOptimizePath;
    private String mPackageName;

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public int getIsOptimizeEnable() {
        return this.mIsOptimizeEnable;
    }

    public void setIsOptimizeEnable(int mIsOptimizeEnable) {
        this.mIsOptimizeEnable = mIsOptimizeEnable;
    }

    public String[] getOptimizePath() {
        return this.mOptimizePath;
    }

    public void setOptimizePath(String[] mOptimizePath) {
        this.mOptimizePath = mOptimizePath;
    }

    public String toString() {
        return "AppConfig{mPackageName='" + this.mPackageName + "', mIsOptimizeEnable=" + this.mIsOptimizeEnable + ", mOptimizePath=" + Arrays.toString(this.mOptimizePath) + '}';
    }
}
