package com.miui.server.input.stylus;

import java.util.Collections;
import java.util.Set;

/* loaded from: classes.dex */
public class StylusPageKeyConfig {
    private int mVersion = Integer.MIN_VALUE;
    private boolean mEnable = true;
    private Set<String> mAppWhiteSet = Collections.emptySet();
    private Set<String> mActivityBlackSet = Collections.emptySet();

    public int getVersion() {
        return this.mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void setEnable(boolean enable) {
        this.mEnable = enable;
    }

    public Set<String> getAppWhiteSet() {
        return this.mAppWhiteSet;
    }

    public void setAppWhiteSet(Set<String> appWhiteSet) {
        this.mAppWhiteSet = appWhiteSet;
    }

    public Set<String> getActivityBlackSet() {
        return this.mActivityBlackSet;
    }

    public void setActivityBlackSet(Set<String> activityBlackSet) {
        this.mActivityBlackSet = activityBlackSet;
    }

    public String toString() {
        return "StylusPageKeyConfig{version=" + this.mVersion + ", enable=" + this.mEnable + ", appWhiteSet=" + this.mAppWhiteSet + ", activityBlackSet=" + this.mActivityBlackSet + '}';
    }
}
