package com.android.server.display;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.MathUtils;
import android.util.Slog;
import android.view.Display;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.brightness.BrightnessReason;
import java.io.PrintWriter;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class RampRateController {
    private static final int ANIMATION_RATE_TYPE_DEFAULT = 0;
    private static final int ANIMATION_RATE_TYPE_DIM = 1;
    private static final int ANIMATION_RATE_TYPE_HDR_BRIGHTNESS = 4;
    private static final int ANIMATION_RATE_TYPE_MANUAL_BRIGHTNESS = 3;
    private static final int ANIMATION_RATE_TYPE_TEMPORARY_DIMMING = 2;
    private static final int BRIGHTNESS_12BIT = 4095;
    private static final boolean IS_UMI_0B_DISPLAY_PANEL;
    private static final float NIT_LEVEL = 40.0f;
    private static final float NIT_LEVEL1 = 35.0f;
    private static final float NIT_LEVEL2 = 87.450005f;
    private static final float NIT_LEVEL3 = 265.0f;
    private static final String OLED_PANEL_ID;
    private static final int RATE_LEVEL = 40;
    private static final int REASON_AUTO_BRIGHTNESS_RATE = 8;
    private static final int REASON_BCBC_BRIGHTNESS_RATE = 11;
    private static final int REASON_DIM_BRIGHTNESS_RATE = 3;
    private static final int REASON_DOZE_BRIGHTNESS_RATE = 12;
    private static final int REASON_FAST_BRIGHTNESS_ADJUSTMENT_RATE = 7;
    private static final int REASON_FAST_RATE = 2;
    private static final int REASON_HDR_BRIGHTNESS_RATE = 6;
    private static final int REASON_MANUAL_BRIGHTNESS_RATE = 9;
    private static final int REASON_OVERRIDE_BRIGHTNESS_RATE = 5;
    private static final int REASON_SLOW_FAST = 1;
    private static final int REASON_TEMPORARY_BRIGHTNESS_RATE = 4;
    private static final int REASON_THERMAL_BRIGHTNESS_RATE = 10;
    private static final int REASON_ZERO_RATE = 0;
    private static final String TAG = RampRateController.class.getSimpleName();
    private static final float TIME_1 = 0.0f;
    private static final float TIME_2 = 0.8f;
    private static final float TIME_3 = 1.8f;
    private static final float TIME_4 = 4.0f;
    private static final float TIME_5 = 24.0f;
    private boolean mAnimateValueChanged;
    private final int mAnimationDurationDim;
    private final int mAnimationDurationDozeDimming;
    private final int mAnimationDurationHdrBrightness;
    private final int mAnimationDurationManualBrightness;
    private final int mAnimationDurationTemporaryDimming;
    private boolean mAppliedHdrRateDueToEntry;
    private boolean mAppliedHdrRateDueToExit;
    private boolean mAppliedOverrideRateDueToEntry;
    private boolean mAppliedOverrideRateDueToExit;
    private float mBrightness;
    private boolean mBrightnessChanged;
    private final float mBrightnessRampRateFast;
    private final float mBrightnessRampRateSlow;
    private int mCurrentPolicy;
    private final DisplayDeviceConfig mDisplayDeviceConfig;
    private final DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private final Handler mHandler;
    private final DisplayDeviceConfig.HighBrightnessModeData mHighBrightnessModeData;
    private final float mMinimumAdjustRate;
    private int mPreviousDisplayState;
    private int mPreviousPolicy;
    private RateStateRecord mRateRecord;
    private final int mScreenBrightnessMaximumInt;
    private float mSdrBrightness;
    private boolean mSlowChange;
    private final float mSlowLinearRampRate;
    private final boolean mSupportManualDimming;
    private float mTargetBrightness;
    private float mTargetSdrBrightness;
    private final float[] mNitsLevels = {800.0f, 251.0f, 150.0f, 100.0f, 70.0f, 50.0f, NIT_LEVEL, 30.0f, 28.5f};
    private final float[] mALevels = {800.0f, 569.48f, 344.89f, 237.75f, 179.71f, 135.19f, 113.59f, 62.84f, 676.87f};
    private final float[] mBLevels = {0.9887f, 0.992f, 0.995f, 0.9965f, 0.9973f, 0.9979f, 0.9982f, 0.999f, 0.996f};
    private final BrightnessReason mBrightnessReason = new BrightnessReason();
    private final BrightnessReason mBrightnessReasonTemp = new BrightnessReason();
    private boolean mDebug = false;
    private float mHbmTransitionPointNit = Float.NaN;
    private float mHbmMaxNit = Float.NaN;
    private float mBrightnessMaxNit = Float.NaN;

    static {
        String str = SystemProperties.get("ro.boot.oled_panel_id", "");
        OLED_PANEL_ID = str;
        IS_UMI_0B_DISPLAY_PANEL = "0B".equals(str) && ("umi".equals(Build.DEVICE) || "umiin".equals(Build.DEVICE));
    }

    public RampRateController(Context context, DisplayPowerControllerImpl impl, DisplayDeviceConfig deviceConfig, Looper looper) {
        this.mDisplayPowerControllerImpl = impl;
        this.mDisplayDeviceConfig = deviceConfig;
        this.mHandler = new Handler(looper);
        this.mHighBrightnessModeData = deviceConfig.getHighBrightnessModeData();
        Resources resources = context.getResources();
        this.mBrightnessRampRateSlow = resources.getFloat(R.dimen.config_screenBrightnessSettingDefaultFloat);
        this.mBrightnessRampRateFast = resources.getFloat(R.dimen.config_screenBrightnessDozeFloat);
        this.mAnimationDurationDim = resources.getInteger(285933578);
        this.mAnimationDurationTemporaryDimming = resources.getInteger(285933582);
        this.mAnimationDurationManualBrightness = resources.getInteger(285933581);
        this.mAnimationDurationHdrBrightness = resources.getInteger(285933580);
        this.mAnimationDurationDozeDimming = resources.getInteger(285933579);
        this.mSlowLinearRampRate = resources.getFloat(285671478);
        int integer = resources.getInteger(R.integer.thumbnail_width_tv);
        this.mScreenBrightnessMaximumInt = integer;
        this.mMinimumAdjustRate = 1.0f / integer;
        this.mSupportManualDimming = FeatureParser.getBoolean("support_manual_dimming", false);
        this.mRateRecord = new RateStateRecord();
        updateDeviceConfigData();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateBrightnessState(boolean animating, boolean readyToAnimate, boolean slowChange, float brightness, float sdrBrightness, float targetBrightness, float targetSdrBrightness, int currentPolicy, int previousPolicy, int oldDisplayState, BrightnessReason reasonTemp, BrightnessReason reason) {
        this.mSlowChange = slowChange;
        this.mBrightness = brightness;
        this.mSdrBrightness = sdrBrightness;
        this.mTargetBrightness = targetBrightness;
        this.mTargetSdrBrightness = targetSdrBrightness;
        this.mCurrentPolicy = currentPolicy;
        this.mPreviousPolicy = previousPolicy;
        this.mPreviousDisplayState = oldDisplayState;
        this.mBrightnessReasonTemp.set(reasonTemp);
        this.mBrightnessReason.set(reason);
        updateRatePriority(reasonTemp, reason, targetBrightness, targetSdrBrightness, animating, readyToAnimate);
    }

    private void updateRatePriority(BrightnessReason reasonTemp, BrightnessReason reason, float targetBrightness, float targetSdrBrightness, boolean animating, boolean readyToAnimate) {
        int i = this.mCurrentPolicy;
        boolean isPolicyBright = i == 3;
        boolean isPolicyDim = i == 2;
        boolean isReasonManual = reasonTemp.getReason() == 1;
        boolean isModifierBcbc = (reason.getModifier() & 16) != 0;
        boolean isModifierTempBcbc = (reasonTemp.getModifier() & 16) != 0;
        boolean isModifierThermal = (reason.getModifier() & 64) != 0;
        boolean isModifierTempThermal = (reasonTemp.getModifier() & 64) != 0;
        boolean isModifierTempHdr = (reasonTemp.getModifier() & 4) != 0;
        boolean isReasonTempOverride = reasonTemp.getReason() == 6;
        boolean isReasonTemporary = reasonTemp.getReason() == 7;
        if ((isPolicyBright && (isModifierTempBcbc || isModifierBcbc)) || this.mRateRecord.appliedBcbcDimming()) {
            if (!this.mBrightnessChanged && !animating) {
                this.mRateRecord.addRateModifier(1);
            } else if (this.mRateRecord.appliedBcbcDimming() && (this.mBrightnessChanged || isReasonManual)) {
                this.mRateRecord.clearRateModifier(1);
            }
        }
        if ((isPolicyBright && (isModifierTempThermal || isModifierThermal)) || this.mRateRecord.appliedThermalDimming()) {
            if (!this.mBrightnessChanged && !this.mAnimateValueChanged && !animating) {
                this.mRateRecord.clearRateModifier(1);
                this.mRateRecord.addRateModifier(2);
            } else if (this.mRateRecord.appliedThermalDimming() && (this.mBrightnessChanged || this.mAnimateValueChanged)) {
                this.mRateRecord.clearRateModifier(2);
            }
        }
        if (isPolicyBright && (this.mRateRecord.appliedHdrDimming() || (isModifierTempHdr && this.mAnimateValueChanged))) {
            this.mRateRecord.clearRateModifier(3);
            this.mRateRecord.addRateModifier(8);
        }
        if (isPolicyBright && (this.mRateRecord.appliedOverrideDimming() || (isReasonTempOverride && this.mBrightnessChanged))) {
            this.mRateRecord.clearRateModifier(11);
            this.mRateRecord.addRateModifier(16);
        }
        long currentTime = SystemClock.elapsedRealtime();
        if ((!isPolicyBright || !Display.isDozeState(this.mPreviousDisplayState) || targetBrightness <= this.mBrightness) && !this.mRateRecord.appliedDozeDimming(currentTime)) {
            if (this.mRateRecord.appliedDozeDimming()) {
                this.mRateRecord.clearRateModifier(4);
            }
        } else {
            this.mRateRecord.clearRateModifier(27);
            if (readyToAnimate) {
                this.mRateRecord.setDozeDimmingTimeMills(currentTime);
                this.mRateRecord.addRateModifier(4);
            }
        }
        if ((isPolicyBright && isReasonTemporary) || this.mRateRecord.appliedTemporaryDimming()) {
            long now = SystemClock.elapsedRealtime();
            this.mRateRecord.clearRateModifier(31);
            if (this.mSupportManualDimming) {
                if (!isReasonTemporary) {
                    if (this.mRateRecord.appliedTemporaryDimming(now, targetBrightness, targetSdrBrightness)) {
                        this.mRateRecord.addRateModifier(32);
                    } else {
                        this.mRateRecord.clearRateModifier(32);
                    }
                } else {
                    this.mRateRecord.setStartTemporaryDimmingTimeMills(now);
                    this.mRateRecord.addRateModifier(32);
                }
            }
        }
        if (isPolicyDim || (this.mPreviousPolicy == 2 && isPolicyBright)) {
            this.mRateRecord.clearRateModifier(63);
            this.mRateRecord.addRateModifier(64);
        } else if (this.mRateRecord.appliedDimDimming()) {
            this.mRateRecord.clearRateModifier(64);
        }
        if (targetBrightness == 0.0f || targetSdrBrightness == 0.0f || this.mBrightness == 0.0f || this.mSdrBrightness == 0.0f) {
            this.mRateRecord.clearRateModifier(RateStateRecord.MODIFIER_RATE_ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public float updateBrightnessRate(String name, float currentBrightness, float targetBrightness, float rate) {
        float finalRate = rate;
        float startBrightness = this.mBrightness;
        if (DisplayPowerState.SCREEN_SDR_BRIGHTNESS_FLOAT.getName().equals(name)) {
            startBrightness = this.mSdrBrightness;
        }
        boolean increase = targetBrightness > startBrightness;
        this.mRateRecord.resetRateType();
        RateStateRecord rateStateRecord = this.mRateRecord;
        rateStateRecord.setPreviousRateReason(rateStateRecord.getCurrentRateReason(name), name);
        if (this.mSlowChange) {
            this.mRateRecord.setCurrentRateReason(1, name);
        } else {
            this.mRateRecord.setCurrentRateReason(2, name);
        }
        if (this.mRateRecord.appliedDimDimming()) {
            this.mRateRecord.setRateTypeIfNeeded(1);
            this.mRateRecord.setRateReasonIfNeeded(3, name);
        } else if (this.mRateRecord.appliedDozeDimming()) {
            this.mRateRecord.setCurrentRateReason(12, name);
            finalRate = ((targetBrightness - startBrightness) * 1000.0f) / this.mAnimationDurationDozeDimming;
        } else if (this.mRateRecord.appliedTemporaryDimming()) {
            this.mRateRecord.setCurrentRateReason(0, name);
            this.mRateRecord.setRateTypeIfNeeded(2);
            this.mRateRecord.setRateReasonIfNeeded(4, name);
        } else if (this.mRateRecord.appliedOverrideDimming()) {
            this.mRateRecord.setRateTypeIfNeeded(3);
            this.mRateRecord.setRateReasonIfNeeded(5, name);
        } else if (this.mRateRecord.appliedHdrDimming()) {
            this.mRateRecord.setRateType(4);
            this.mRateRecord.setCurrentRateReason(6, name);
        } else if (this.mRateRecord.appliedThermalDimming()) {
            this.mRateRecord.setCurrentRateReason(10, name);
            finalRate = this.mSlowLinearRampRate;
        } else if (this.mSlowChange && this.mRateRecord.appliedBcbcDimming()) {
            this.mRateRecord.setCurrentRateReason(11, name);
            finalRate = this.mSlowLinearRampRate;
        } else if (this.mSlowChange && appliedFastRate(currentBrightness, targetBrightness)) {
            this.mRateRecord.setRateTypeIfNeeded(3);
            this.mRateRecord.setCurrentRateReason(7, name);
        } else if (this.mSlowChange && finalRate != 0.0f) {
            this.mRateRecord.setCurrentRateReason(8, name);
            finalRate = getRampRate(increase, startBrightness, currentBrightness, targetBrightness);
        } else if (finalRate != 0.0f) {
            this.mRateRecord.setRateTypeIfNeeded(3);
            this.mRateRecord.setRateReasonIfNeeded(9, name);
        } else {
            this.mRateRecord.setCurrentRateReason(0, name);
        }
        if (BrightnessSynchronizer.brightnessFloatToInt(currentBrightness) == BrightnessSynchronizer.brightnessFloatToInt(targetBrightness) && finalRate != 0.0f) {
            float finalRate2 = this.mSlowChange ? this.mBrightnessRampRateSlow : this.mBrightnessRampRateFast;
            if (this.mDebug) {
                Slog.d(TAG, "updateBrightnessRate: " + name + ": using current rate to avoid frequent animation execution: rate: " + finalRate2 + ", currentBrightness: " + currentBrightness + ", targetBrightness: " + targetBrightness);
            }
            return finalRate2;
        }
        float finalRate3 = getRampRate(this.mRateRecord.getRateType(), increase, startBrightness, currentBrightness, targetBrightness, finalRate);
        int previousRateReason = this.mRateRecord.getPreviousRateReason(name);
        int currentRateReason = this.mRateRecord.getCurrentRateReason(name);
        if (currentRateReason != previousRateReason || this.mDebug) {
            Slog.d(TAG, "updateBrightnessRate: " + name + ": brightness rate changing from [" + reasonToString(previousRateReason) + "] to [" + reasonToString(currentRateReason) + "], rate: " + finalRate3);
        }
        return finalRate3;
    }

    private float getRampRate(boolean increase, float startBrightness, float currentBrightness, float targetBrightness) {
        if (increase) {
            return getBrighteningRate(currentBrightness, startBrightness, targetBrightness);
        }
        return getDarkeningRate(currentBrightness);
    }

    private float getBrighteningRate(float brightness, float startBrightness, float tgtBrightness) {
        float rate;
        float nit = convertToNit(brightness);
        float startNit = convertToNit(startBrightness);
        float targetNit = convertToNit(tgtBrightness);
        if (startNit < NIT_LEVEL1) {
            if (targetNit < NIT_LEVEL1) {
                rate = convertToBrightness(targetNit - startNit) / TIME_4;
            } else if (targetNit < NIT_LEVEL3) {
                if (nit < NIT_LEVEL1) {
                    rate = (convertToBrightness(NIT_LEVEL1) - startBrightness) / TIME_3;
                } else {
                    rate = getExpRate(NIT_LEVEL1, targetNit, nit, TIME_3, TIME_4);
                }
            } else if (nit < NIT_LEVEL1) {
                rate = (convertToBrightness(NIT_LEVEL1) - startBrightness) / TIME_2;
            } else if (nit < NIT_LEVEL2) {
                rate = getExpRate(NIT_LEVEL1, NIT_LEVEL2, nit, TIME_2, TIME_3);
            } else {
                rate = getExpRate(NIT_LEVEL2, targetNit, nit, TIME_3, TIME_4);
            }
        } else if (startNit >= NIT_LEVEL3) {
            rate = getExpRate(startNit, targetNit, nit, 0.0f, TIME_4);
        } else if (targetNit < NIT_LEVEL3) {
            rate = getExpRate(startNit, targetNit, nit, 0.0f, TIME_4);
        } else if (nit < NIT_LEVEL2) {
            rate = getExpRate(startNit, NIT_LEVEL2, nit, 0.0f, TIME_3);
        } else {
            rate = getExpRate(NIT_LEVEL2, targetNit, nit, TIME_3, TIME_4);
        }
        float rate2 = MathUtils.max(rate, this.mMinimumAdjustRate);
        if (this.mDebug) {
            Slog.d(TAG, "getBrighteningRate: rate: " + rate2 + ", nit: " + nit + ", startNit: " + startNit + ", targetNit: " + targetNit);
        }
        return rate2;
    }

    private float getExpRate(float startNit, float targetNit, float currentNit, float startTime, float targetTime) {
        float beginDbv = convertToBrightness(startNit);
        float endDbv = convertToBrightness(targetNit);
        float curDbv = convertToBrightness(currentNit);
        float a = MathUtils.log(endDbv / beginDbv) / (targetTime - startTime);
        return a * curDbv;
    }

    private float getDarkeningRate(float brightness) {
        float rate;
        DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = this.mHighBrightnessModeData;
        float normalMaxBrightnessFloat = highBrightnessModeData == null ? 1.0f : highBrightnessModeData.transitionPoint;
        float normalNit = this.mDisplayDeviceConfig.getNitsFromBacklight(normalMaxBrightnessFloat);
        if (normalNit == -1.0f) {
            normalNit = 500.0f;
        }
        int normalMaxBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(normalMaxBrightnessFloat);
        float nit = convertToNit(brightness);
        int index = getIndex(nit);
        float rate2 = MathUtils.abs(this.mALevels[index] * TIME_5 * MathUtils.pow(this.mBLevels[index], getTime(nit, index) * TIME_5) * MathUtils.log(this.mBLevels[index]));
        if (nit > this.mHbmTransitionPointNit) {
            int maxBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(1.0f);
            rate = (BrightnessSynchronizer.brightnessIntToFloat(maxBrightnessInt - normalMaxBrightnessInt) * rate2) / (this.mHbmMaxNit - this.mHbmTransitionPointNit);
        } else {
            rate = (rate2 * normalMaxBrightnessFloat) / normalNit;
        }
        if (normalMaxBrightnessInt < 4095 && nit <= NIT_LEVEL) {
            rate = NIT_LEVEL / this.mScreenBrightnessMaximumInt;
        } else if (IS_UMI_0B_DISPLAY_PANEL && nit <= NIT_LEVEL) {
            rate = 80.0f / this.mScreenBrightnessMaximumInt;
        }
        float rate3 = MathUtils.max(rate, this.mMinimumAdjustRate);
        if (this.mDebug) {
            Slog.d(TAG, "getDarkeningRate: rate: " + rate3 + ", nit: " + nit);
        }
        return rate3;
    }

    private int getIndex(float nit) {
        int index = 1;
        while (true) {
            float[] fArr = this.mNitsLevels;
            if (fArr.length <= index || nit >= fArr[index]) {
                break;
            }
            index++;
        }
        if (this.mDebug) {
            Slog.d(TAG, "getIndex: nit: " + nit + ", index: " + index);
        }
        return index - 1;
    }

    private float getTime(float nit, int index) {
        float a = this.mALevels[index];
        float b = this.mBLevels[index];
        float time = (MathUtils.log(nit / a) / MathUtils.log(b)) / TIME_5;
        if (this.mDebug) {
            Slog.d(TAG, "getTime: time: " + time + ", a: " + a + ", b: " + b);
        }
        return MathUtils.abs(time);
    }

    private void updateDeviceConfigData() {
        DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = this.mHighBrightnessModeData;
        if (highBrightnessModeData != null && this.mDisplayPowerControllerImpl != null) {
            this.mHbmTransitionPointNit = convertToNit(highBrightnessModeData.transitionPoint);
            this.mBrightnessMaxNit = convertToNit(1.0f);
            this.mHbmMaxNit = convertToNit(this.mDisplayPowerControllerImpl.getMaxHbmBrightnessForPeak());
            return;
        }
        float[] nits = this.mDisplayDeviceConfig.getNits();
        if (nits != null && nits.length != 0) {
            this.mBrightnessMaxNit = nits[nits.length - 1];
        } else {
            this.mBrightnessMaxNit = 500.0f;
            Slog.e(TAG, "The max nit of the device is not adapted.");
        }
        this.mHbmMaxNit = this.mBrightnessMaxNit;
    }

    private float getRampRate(int type, boolean increase, float startBrightness, float currentBrightness, float targetBrightness, float rate) {
        float duration = getRateDuration(type);
        if (Float.isNaN(duration) || targetBrightness == 0.0f) {
            if (this.mDebug) {
                Slog.d(TAG, "getRampRate: rate: " + rate);
            }
            return rate;
        }
        if (increase) {
            return getBrighteningLogRate(startBrightness, currentBrightness, targetBrightness, duration);
        }
        return getDarkeningExpRate(startBrightness, currentBrightness, targetBrightness, duration);
    }

    private float getBrighteningLogRate(float startBrightness, float currentBrightness, float targetBrightness, float duration) {
        float coefficient = ((MathUtils.exp(targetBrightness) - MathUtils.exp(startBrightness)) * 1000.0f) / duration;
        float rate = coefficient / MathUtils.exp(currentBrightness);
        if (this.mDebug) {
            Slog.d(TAG, "getBrighteningLogRate: rate: " + rate + ", startBrightness: " + startBrightness + ", currentBrightness: " + currentBrightness + ", targetBrightness: " + targetBrightness + ", coefficient: " + coefficient);
        }
        return rate;
    }

    private float getDarkeningExpRate(float startBrightness, float currentBrightness, float targetBrightness, float duration) {
        float coefficient = (MathUtils.log(targetBrightness / startBrightness) * 1000.0f) / duration;
        float rate = MathUtils.abs(coefficient * currentBrightness);
        if (this.mDebug) {
            Slog.d(TAG, "getDarkeningExpRate: rate: " + rate + ", startBrightness: " + startBrightness + ", currentBrightness: " + currentBrightness + ", targetBrightness: " + targetBrightness + ", coefficient: " + coefficient);
        }
        return rate;
    }

    private float getRateDuration(int type) {
        switch (type) {
            case 1:
                return this.mAnimationDurationDim;
            case 2:
                return this.mAnimationDurationTemporaryDimming;
            case 3:
                return this.mAnimationDurationManualBrightness;
            case 4:
                return this.mAnimationDurationHdrBrightness;
            default:
                return Float.NaN;
        }
    }

    private float convertToNit(float brightness) {
        DisplayDeviceConfig displayDeviceConfig = this.mDisplayDeviceConfig;
        if (displayDeviceConfig == null) {
            return Float.NaN;
        }
        return displayDeviceConfig.getNitsFromBacklight(brightness);
    }

    private float convertToBrightness(float nit) {
        DisplayDeviceConfig displayDeviceConfig = this.mDisplayDeviceConfig;
        if (displayDeviceConfig == null) {
            return Float.NaN;
        }
        return displayDeviceConfig.getBrightnessFromNit(nit);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onBrightnessChanged(boolean changed, boolean isAnimating) {
        if (!changed && !isAnimating && ((this.mAppliedOverrideRateDueToEntry || this.mAppliedOverrideRateDueToExit) && this.mRateRecord.appliedOverrideDimming())) {
            clearOverrideRateModifier();
        }
        this.mBrightnessChanged = changed;
        this.mAppliedOverrideRateDueToEntry = false;
        this.mAppliedOverrideRateDueToExit = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onAnimateValueChanged(boolean changed, boolean isAnimating) {
        if (!changed && !isAnimating && ((this.mAppliedHdrRateDueToEntry || this.mAppliedHdrRateDueToExit) && this.mRateRecord.appliedHdrDimming())) {
            clearHdrRateModifier();
        }
        this.mAnimateValueChanged = changed;
        this.mAppliedHdrRateDueToEntry = false;
        this.mAppliedHdrRateDueToExit = false;
    }

    private boolean appliedFastRate(float currentBrightness, float targetBrightness) {
        return this.mDisplayPowerControllerImpl.appliedFastRate(currentBrightness, targetBrightness);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isTemporaryDimming() {
        return this.mRateRecord.appliedTemporaryDimming();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearBcbcModifier() {
        this.mRateRecord.clearRateModifier(1);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearThermalModifier() {
        this.mRateRecord.clearRateModifier(2);
    }

    private void clearHdrRateModifier() {
        this.mRateRecord.clearRateModifier(8);
    }

    private void clearOverrideRateModifier() {
        this.mRateRecord.clearRateModifier(16);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearAllRateModifier() {
        this.mRateRecord.clearRateModifier(RateStateRecord.MODIFIER_RATE_ALL);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addHdrRateModifier(boolean isEntry) {
        this.mRateRecord.clearRateModifier(RateStateRecord.MODIFIER_RATE_ALL);
        this.mRateRecord.addRateModifier(8);
        if (isEntry) {
            this.mAppliedHdrRateDueToEntry = true;
        } else {
            this.mAppliedHdrRateDueToExit = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addOverrideRateModifier(boolean isEntry) {
        this.mRateRecord.clearRateModifier(RateStateRecord.MODIFIER_RATE_ALL);
        this.mRateRecord.addRateModifier(16);
        if (isEntry) {
            this.mAppliedOverrideRateDueToEntry = true;
        } else {
            this.mAppliedOverrideRateDueToExit = true;
        }
    }

    private String reasonToString(int reason) {
        switch (reason) {
            case 0:
                return "zero_rate";
            case 1:
                return "slow_fast";
            case 2:
                return "fast_rate";
            case 3:
                return "dim_rate";
            case 4:
                return "temporary_brightness_rate";
            case 5:
                return "override_brightness_rate";
            case 6:
                return "hdr_brightness_rate";
            case 7:
                return "fast_brightness_adj";
            case 8:
                return "auto_brightness_rate";
            case 9:
                return "manual_brightness_rate";
            case 10:
                return "thermal_rate";
            case 11:
                return "bcbc_rate";
            case 12:
                return "doze_rate";
            default:
                return "unknown";
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        this.mDebug = DisplayDebugConfig.DEBUG_DPC;
        pw.println();
        pw.println("Ramp Rate Controller:");
        pw.println("  mRateType=" + this.mRateRecord.getRateType());
        StringBuilder append = new StringBuilder().append("  mCurrentRateReason=");
        RateStateRecord rateStateRecord = this.mRateRecord;
        pw.println(append.append(rateStateRecord.getCurrentRateReason(rateStateRecord.SCREEN_BRIGHTNESS_FLOAT_NAME)).toString());
        StringBuilder append2 = new StringBuilder().append("  mPreviousRateReason=");
        RateStateRecord rateStateRecord2 = this.mRateRecord;
        pw.println(append2.append(rateStateRecord2.getPreviousRateReason(rateStateRecord2.SCREEN_BRIGHTNESS_FLOAT_NAME)).toString());
        StringBuilder append3 = new StringBuilder().append("  mCurrentSdrRateReason=");
        RateStateRecord rateStateRecord3 = this.mRateRecord;
        pw.println(append3.append(rateStateRecord3.getCurrentRateReason(rateStateRecord3.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME)).toString());
        StringBuilder append4 = new StringBuilder().append("  mPreviousSdrRateReason=");
        RateStateRecord rateStateRecord4 = this.mRateRecord;
        pw.println(append4.append(rateStateRecord4.getPreviousRateReason(rateStateRecord4.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME)).toString());
        pw.println("  mRateModifier=" + this.mRateRecord.getRateModifier());
    }

    /* loaded from: classes.dex */
    public final class RateStateRecord {
        public static final int MODIFIER_RATE_ALL = 127;
        public static final int MODIFIER_RATE_BCBC = 1;
        public static final int MODIFIER_RATE_DIM = 64;
        public static final int MODIFIER_RATE_DOZE = 4;
        public static final int MODIFIER_RATE_HDR = 8;
        public static final int MODIFIER_RATE_OVERRIDE = 16;
        public static final int MODIFIER_RATE_TEMPORARY = 32;
        public static final int MODIFIER_RATE_THERMAL = 2;
        private final String SCREEN_BRIGHTNESS_FLOAT_NAME = DisplayPowerState.SCREEN_BRIGHTNESS_FLOAT.getName();
        private final String SCREEN_SDR_BRIGHTNESS_FLOAT_NAME = DisplayPowerState.SCREEN_SDR_BRIGHTNESS_FLOAT.getName();
        private int mCurrentRateReason;
        private int mCurrentSdrRateReason;
        private int mPreviousRateReason;
        private int mPreviousSdrRateReason;
        private int mRateModifier;
        private int mRateType;
        private long mStartDozeDimmingTimeMills;
        private long mStartTemporaryDimmingTimeMills;

        public RateStateRecord() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addRateModifier(int modifier) {
            this.mRateModifier |= modifier;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clearRateModifier(int modifier) {
            this.mRateModifier &= ~modifier;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setRateTypeIfNeeded(int type) {
            if (RampRateController.this.mSupportManualDimming) {
                setRateType(type);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setRateType(int type) {
            this.mRateType = type;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setRateReasonIfNeeded(int reason, String name) {
            if (RampRateController.this.mSupportManualDimming) {
                setCurrentRateReason(reason, name);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setCurrentRateReason(int reason, String name) {
            if (this.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME.equals(name)) {
                this.mCurrentSdrRateReason = reason;
            } else {
                this.mCurrentRateReason = reason;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setPreviousRateReason(int currentRateReason, String name) {
            if (this.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME.equals(name)) {
                this.mPreviousSdrRateReason = currentRateReason;
            } else {
                this.mPreviousRateReason = currentRateReason;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getRateType() {
            return this.mRateType;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getCurrentRateReason(String name) {
            if (this.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME.equals(name)) {
                return this.mCurrentSdrRateReason;
            }
            return this.mCurrentRateReason;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getPreviousRateReason(String name) {
            if (this.SCREEN_SDR_BRIGHTNESS_FLOAT_NAME.equals(name)) {
                return this.mPreviousSdrRateReason;
            }
            return this.mPreviousRateReason;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getRateModifier() {
            return this.mRateModifier;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void resetRateType() {
            this.mRateType = 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setStartTemporaryDimmingTimeMills(long timeMills) {
            if (RampRateController.this.mSupportManualDimming) {
                this.mStartTemporaryDimmingTimeMills = timeMills;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setDozeDimmingTimeMills(long timeMills) {
            this.mStartDozeDimmingTimeMills = timeMills;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedTemporaryDimming() {
            return (this.mRateModifier & 32) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedTemporaryDimming(long timeMills, float targetBrightness, float targetSdrBrightness) {
            if (RampRateController.this.mSupportManualDimming && timeMills - this.mStartTemporaryDimmingTimeMills <= RampRateController.this.mAnimationDurationTemporaryDimming && (targetBrightness != 0.0f || targetSdrBrightness != 0.0f)) {
                Slog.d(RampRateController.TAG, "Continue using rate of temporary dimming.");
                return true;
            }
            this.mStartTemporaryDimmingTimeMills = 0L;
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedHdrDimming() {
            return (this.mRateModifier & 8) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedThermalDimming() {
            return (this.mRateModifier & 2) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedBcbcDimming() {
            return (this.mRateModifier & 1) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedOverrideDimming() {
            return (this.mRateModifier & 16) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedDimDimming() {
            return (this.mRateModifier & 64) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedDozeDimming() {
            return (this.mRateModifier & 4) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean appliedDozeDimming(long timeMills) {
            if (timeMills - this.mStartDozeDimmingTimeMills <= RampRateController.this.mAnimationDurationDozeDimming) {
                Slog.i(RampRateController.TAG, "Continue using rate of doze dimming.");
                return true;
            }
            this.mStartDozeDimmingTimeMills = 0L;
            return false;
        }
    }
}
