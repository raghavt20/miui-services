package com.android.server.tof;

/* loaded from: classes.dex */
public class TofGestureComponent {
    public static final int CATEGORY_LONG_VIDEO = 1;
    public static final int CATEGORY_MUSIC = 3;
    public static final int CATEGORY_OFFICE = 4;
    public static final int CATEGORY_PHONE = 5;
    public static final int CATEGORY_READER = 6;
    public static final int CATEGORY_SHORT_VIDEO = 2;
    public static final int CATEGORY_UNKNOWN = 0;
    public static final int FEATURE_INVALID = 0;
    public static final int FLAG_EVENT_SWIPE_DOWN = 4096;
    public static final int FLAG_EVENT_SWIPE_LEFT = 65536;
    public static final int FLAG_EVENT_SWIPE_RIGHT = 131072;
    public static final int FLAG_EVENT_SWIPE_UP = 2048;
    public static final int FLAG_KEYCODE_CALL = 128;
    public static final int FLAG_KEYCODE_DPAD_LEFT = 4;
    public static final int FLAG_KEYCODE_DPAD_RIGHT = 2;
    public static final int FLAG_KEYCODE_ENDCALL = 256;
    public static final int FLAG_KEYCODE_MEDIA_FAST_FORWARD = 8192;
    public static final int FLAG_KEYCODE_MEDIA_NEXT = 8;
    public static final int FLAG_KEYCODE_MEDIA_PLAY_PAUSE = 1;
    public static final int FLAG_KEYCODE_MEDIA_PREVIOUS = 16;
    public static final int FLAG_KEYCODE_MEDIA_REWIND = 16384;
    public static final int FLAG_KEYCODE_PAGE_DOWN = 32;
    public static final int FLAG_KEYCODE_PAGE_UP = 64;
    public static final int FLAG_KEYCODE_SPACE_MEDIA_PLAY_PAUSE = 32768;
    public static final int FLAG_KEYCODE_VOLUME_DOWN = 1024;
    public static final int FLAG_KEYCODE_VOLUME_UP = 512;
    private String mActivityName;
    private int mCategory;
    private boolean mEnable;
    private int mLandScapeFeature;
    private int mMaxVersionCode;
    private int mMinVersionCode;
    private String mPackageName;
    private int mPortraitFeature;

    public TofGestureComponent() {
    }

    public TofGestureComponent(String pkgName, String activityName, int category, int feature, int featureVertical, int minVersionCode, int mMaxVersionCode) {
        this.mPackageName = pkgName;
        this.mActivityName = activityName;
        this.mCategory = category;
        this.mLandScapeFeature = feature;
        this.mPortraitFeature = featureVertical;
        this.mMinVersionCode = minVersionCode;
        this.mMaxVersionCode = mMaxVersionCode;
    }

    public TofGestureComponent(String pkgName, String activityName, int category, int feature, int featureVertical, int minVersionCode, int maxVersionCode, boolean enable) {
        this.mPackageName = pkgName;
        this.mActivityName = activityName;
        this.mCategory = category;
        this.mLandScapeFeature = feature;
        this.mPortraitFeature = featureVertical;
        this.mMinVersionCode = minVersionCode;
        this.mMaxVersionCode = maxVersionCode;
        this.mEnable = enable;
    }

    public String toString() {
        return "TofGestureComponent{mPackageName='" + this.mPackageName + "', mActivityName='" + this.mActivityName + "', mCategory=" + this.mCategory + ", mLandScapeFeature=" + this.mLandScapeFeature + ", mPortraitFeature=" + this.mPortraitFeature + ", mMinVersionCode=" + this.mMinVersionCode + ", mMaxVersionCode=" + this.mMaxVersionCode + ", mEnable=" + this.mEnable + '}';
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public String getActivityName() {
        return this.mActivityName;
    }

    public void setActivityName(String mActivityName) {
        this.mActivityName = mActivityName;
    }

    public int getCategory() {
        return this.mCategory;
    }

    public void setCategory(int mCategory) {
        this.mCategory = mCategory;
    }

    public int getLandscapeFeature() {
        return this.mLandScapeFeature;
    }

    public void setLandScapeFeature(int mFeature) {
        this.mLandScapeFeature = mFeature;
    }

    public int getPortraitFeature() {
        return this.mPortraitFeature;
    }

    public void setPortraitFeature(int mFeatureVertical) {
        this.mPortraitFeature = mFeatureVertical;
    }

    public int getMinVersionCode() {
        return this.mMinVersionCode;
    }

    public void setMinVersionCode(int minVersionCode) {
        this.mMinVersionCode = minVersionCode;
    }

    public int getMaxVersionCode() {
        return this.mMaxVersionCode;
    }

    public void setMaxVersionCode(int maxVersionCode) {
        this.mMaxVersionCode = maxVersionCode;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void setEnable(boolean mEnable) {
        this.mEnable = mEnable;
    }

    public boolean isVersionSupported(int versionCode) {
        int i = this.mMinVersionCode;
        boolean isMinVersionValid = i > 0;
        int i2 = this.mMaxVersionCode;
        boolean isMaxVersionValid = i2 > 0;
        return (!isMinVersionValid || isMaxVersionValid) ? (isMinVersionValid && isMaxVersionValid) ? versionCode >= i && versionCode <= i2 : (!isMaxVersionValid || isMinVersionValid) ? (isMinVersionValid || isMaxVersionValid) ? false : true : versionCode <= i2 : versionCode >= i;
    }
}
