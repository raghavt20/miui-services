package com.android.server.input.config;

import android.os.Parcel;

/* loaded from: classes.dex */
public final class InputCommonConfig extends BaseInputConfig {
    public static final int CONFIG_TYPE = 2;
    private static volatile InputCommonConfig instance;
    private boolean mCtsMode;
    private boolean mCustomized;
    private boolean mInjectEventStatus;
    private boolean mIsLaserDrawing;
    private boolean mIsUsingMagicPointer;
    private boolean mMouseNaturalScroll;
    private boolean mNeedSyncMotion;
    private boolean mOnewayMode;
    private boolean mPadMode;
    private int mProductId;
    private boolean mRecordEventStatus;
    private boolean mShown;
    private float mSlidGestureHotZoneWidthRate;
    private int mStylusBlockerDelayTime;
    private boolean mStylusBlockerEnable;
    private float mStylusBlockerMoveThreshold;
    private int mSynergyMode;
    private boolean mTapTouchPad;
    private float mTopGestureHotZoneHeightRate;
    private int mVendorId;
    private int mWakeUpMode;
    private boolean mIsFilterInterceptMode = true;
    private boolean mMiInputEventTimeLineEnable = false;

    @Override // com.android.server.input.config.BaseInputConfig
    public /* bridge */ /* synthetic */ void flushToNative() {
        super.flushToNative();
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public /* bridge */ /* synthetic */ long getConfigNativePtr() {
        return super.getConfigNativePtr();
    }

    private InputCommonConfig() {
    }

    public static InputCommonConfig getInstance() {
        if (instance == null) {
            synchronized (InputCommonConfig.class) {
                if (instance == null) {
                    instance = new InputCommonConfig();
                }
            }
        }
        return instance;
    }

    @Override // com.android.server.input.config.BaseInputConfig
    protected void writeToParcel(Parcel dest) {
        dest.writeInt(this.mWakeUpMode);
        dest.writeInt(this.mSynergyMode);
        dest.writeBoolean(this.mInjectEventStatus);
        dest.writeBoolean(this.mRecordEventStatus);
        dest.writeBoolean(this.mShown);
        dest.writeBoolean(this.mCustomized);
        dest.writeBoolean(this.mPadMode);
        dest.writeBoolean(this.mMouseNaturalScroll);
        dest.writeInt(this.mProductId);
        dest.writeInt(this.mVendorId);
        dest.writeBoolean(this.mOnewayMode);
        dest.writeBoolean(this.mTapTouchPad);
        dest.writeBoolean(this.mStylusBlockerEnable);
        dest.writeInt(this.mStylusBlockerDelayTime);
        dest.writeFloat(this.mStylusBlockerMoveThreshold);
        dest.writeBoolean(this.mCtsMode);
        dest.writeBoolean(this.mNeedSyncMotion);
        dest.writeFloat(this.mTopGestureHotZoneHeightRate);
        dest.writeFloat(this.mSlidGestureHotZoneWidthRate);
        dest.writeBoolean(this.mIsLaserDrawing);
        dest.writeBoolean(this.mIsFilterInterceptMode);
        dest.writeBoolean(this.mMiInputEventTimeLineEnable);
        dest.writeBoolean(this.mIsUsingMagicPointer);
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public int getConfigType() {
        return 2;
    }

    public void setWakeUpMode(int wakeUpMode) {
        this.mWakeUpMode = wakeUpMode;
    }

    public void setSynergyMode(int synergyMode) {
        this.mSynergyMode = synergyMode;
    }

    public void setInjectEventStatus(boolean injectEventStatus) {
        this.mInjectEventStatus = injectEventStatus;
    }

    public void setRecordEventStatus(boolean recordEventStatus) {
        this.mRecordEventStatus = recordEventStatus;
    }

    public void setInputMethodStatus(boolean shown, boolean customized) {
        this.mShown = shown;
        this.mCustomized = customized;
    }

    public void setPadMode(boolean padMode) {
        this.mPadMode = padMode;
    }

    public void setMouseNaturalScrollStatus(boolean mouseNaturalScroll) {
        this.mMouseNaturalScroll = mouseNaturalScroll;
    }

    public void setMiuiKeyboardInfo(int vendorId, int productId) {
        this.mVendorId = vendorId;
        this.mProductId = productId;
    }

    public void setOnewayMode(boolean onewayMode) {
        this.mOnewayMode = onewayMode;
    }

    public void setTapTouchPad(boolean enable) {
        this.mTapTouchPad = enable;
    }

    public void setStylusBlockerEnable(boolean stylusBlockerEnable) {
        this.mStylusBlockerEnable = stylusBlockerEnable;
    }

    public void setStylusBlockerDelayTime(int stylusBlockerDelayTime) {
        this.mStylusBlockerDelayTime = stylusBlockerDelayTime;
    }

    public void setStylusBlockerMoveThreshold(float stylusBlockerMoveThreshold) {
        this.mStylusBlockerMoveThreshold = stylusBlockerMoveThreshold;
    }

    public void setCtsMode(boolean ctsMode) {
        this.mCtsMode = ctsMode;
    }

    public void setNeedSyncMotion(boolean needSyncMotion) {
        this.mNeedSyncMotion = needSyncMotion;
    }

    public void setTopGestureHotZoneHeightRate(float rate) {
        this.mTopGestureHotZoneHeightRate = rate;
    }

    public void setSlidGestureHotZoneWidthRate(float rate) {
        this.mSlidGestureHotZoneWidthRate = rate;
    }

    public void setLaserIsDrawing(boolean isDrawing) {
        this.mIsLaserDrawing = isDrawing;
    }

    public void setFilterInterceptMode(boolean interceptMode) {
        this.mIsFilterInterceptMode = interceptMode;
    }

    public void setMiInputEventTimeLineMode(boolean isMiInputEventTimeLineEnable) {
        this.mMiInputEventTimeLineEnable = isMiInputEventTimeLineEnable;
    }

    public void setIsUsingMagicPointer(boolean isUsingMagicPointer) {
        this.mIsUsingMagicPointer = isUsingMagicPointer;
    }
}
