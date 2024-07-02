package com.android.server.tof;

/* loaded from: classes.dex */
public class TofGestureAppInfo {
    private int mCategory;
    private String mComponentName;
    private boolean mEnable;
    private int mFeature;
    private boolean mShowInSettings;

    public TofGestureAppInfo() {
    }

    public TofGestureAppInfo(String mComponentName, int mCategory, int mFeature, boolean mShowInSettings, boolean mEnable) {
        this.mComponentName = mComponentName;
        this.mCategory = mCategory;
        this.mFeature = mFeature;
        this.mShowInSettings = mShowInSettings;
        this.mEnable = mEnable;
    }

    public String getComponentName() {
        return this.mComponentName;
    }

    public void setComponentName(String mComponentName) {
        this.mComponentName = mComponentName;
    }

    public int getCategory() {
        return this.mCategory;
    }

    public void setCategory(int mCategory) {
        this.mCategory = mCategory;
    }

    public int getFeature() {
        return this.mFeature;
    }

    public void setFeature(int mFeature) {
        this.mFeature = mFeature;
    }

    public boolean isShowInSettings() {
        return this.mShowInSettings;
    }

    public void setShowInSettings(boolean mShowInSettings) {
        this.mShowInSettings = mShowInSettings;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void setEnable(boolean mEnable) {
        this.mEnable = mEnable;
    }

    public String toString() {
        return "TofGestureAppInfo{mComponentName='" + this.mComponentName + "', mAllPageSupport=" + this.mCategory + ", mFeature=" + this.mFeature + ", mShowInSettings=" + this.mShowInSettings + ", mEnable=" + this.mEnable + '}';
    }
}
