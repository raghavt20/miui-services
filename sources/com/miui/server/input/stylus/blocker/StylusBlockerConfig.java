package com.miui.server.input.stylus.blocker;

import java.util.Collections;
import java.util.Set;

/* loaded from: classes.dex */
public class StylusBlockerConfig {
    private int mVersion = Integer.MIN_VALUE;
    private Set<String> mBlockSet = Collections.emptySet();
    private boolean mEnable = true;
    private int mStrongModeDelayTime = 64;
    private float mStrongModeMoveThreshold = 8.0f;
    private int mWeakModeDelayTime = 40;
    private float mWeakModeMoveThreshold = 8.0f;

    public Set<String> getBlockSet() {
        return this.mBlockSet;
    }

    public void setBlockSet(Set<String> blockSet) {
        this.mBlockSet = blockSet;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void setEnable(boolean enable) {
        this.mEnable = enable;
    }

    public int getStrongModeDelayTime() {
        return this.mStrongModeDelayTime;
    }

    public void setStrongModeDelayTime(int strongModeDelayTime) {
        this.mStrongModeDelayTime = strongModeDelayTime;
    }

    public float getStrongModeMoveThreshold() {
        return this.mStrongModeMoveThreshold;
    }

    public void setStrongModeMoveThreshold(float strongModeMoveThreshold) {
        this.mStrongModeMoveThreshold = strongModeMoveThreshold;
    }

    public int getWeakModeDelayTime() {
        return this.mWeakModeDelayTime;
    }

    public void setWeakModeDelayTime(int weakModeDelayTime) {
        this.mWeakModeDelayTime = weakModeDelayTime;
    }

    public float getWeakModeMoveThreshold() {
        return this.mWeakModeMoveThreshold;
    }

    public void setWeakModeMoveThreshold(float weakModeMoveThreshold) {
        this.mWeakModeMoveThreshold = weakModeMoveThreshold;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }
}
