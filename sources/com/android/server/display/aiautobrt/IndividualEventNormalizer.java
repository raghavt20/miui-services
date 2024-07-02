package com.android.server.display.aiautobrt;

import android.util.MathUtils;

/* loaded from: classes.dex */
public class IndividualEventNormalizer {
    private static final float MAX_NORMALIZATION_VALUE = 1.0f;
    private static final float MIN_NORMALIZATION_VALUE = 0.0f;
    private static final int MIXED_ORIENTATION_APP_MAX = 2;
    private static final int MIXED_ORIENTATION_APP_MIN = 0;
    private final float mBrightnessMax;
    private final float mBrightnessMin;
    private final float mBrightnessSpanMax;
    private final float mBrightnessSpanMin;
    private final float mLuxMax;
    private final float mLuxMin;
    private final float mLuxSpanMax;
    private final float mLuxSpanMin;
    private final int mAppIdMin = 0;
    private final int mAppIdMax = 9;
    private final float mMixedOrientationAppMin = 0.0f;
    private final float mMixedOrientationAppMax = 2.0f;

    public IndividualEventNormalizer(float brightnessMin, float normalBrightnessMax, float luxMin, float luxMax, float brightnessSpanMin, float brightnessSpanMax, float luxSpanMin, float luxSpanMax) {
        this.mBrightnessMin = brightnessMin;
        this.mBrightnessMax = normalBrightnessMax;
        this.mLuxMin = luxMin;
        this.mLuxMax = luxMax;
        this.mBrightnessSpanMin = brightnessSpanMin;
        this.mBrightnessSpanMax = brightnessSpanMax;
        this.mLuxSpanMin = luxSpanMin;
        this.mLuxSpanMax = luxSpanMax;
    }

    public float normalizeLux(float lux) {
        if (Float.isNaN(lux)) {
            lux = -1.0f;
        }
        return constrain(MathUtils.norm(this.mLuxMin, this.mLuxMax, Math.min(lux, this.mLuxMax)));
    }

    public float normalizeLuxSpan(float luxSpan) {
        if (Float.isNaN(luxSpan)) {
            luxSpan = -1.0f;
        }
        return constrain(MathUtils.norm(this.mLuxSpanMin, this.mLuxSpanMax, luxSpan));
    }

    public float normalizeNit(float nit) {
        if (Float.isNaN(nit)) {
            nit = -1.0f;
        }
        return constrain(MathUtils.norm(this.mBrightnessMin, this.mBrightnessMax, nit));
    }

    public float normalizeNitSpan(float nitSpan) {
        if (Float.isNaN(nitSpan)) {
            nitSpan = -1.0f;
        }
        return constrain(MathUtils.norm(this.mBrightnessSpanMin, this.mBrightnessSpanMax, nitSpan));
    }

    public float normalizeAppId(int appId) {
        return constrain(MathUtils.norm(this.mAppIdMin, this.mAppIdMax, appId));
    }

    public float normalizedMixedOrientationAppId(int appId, int orientation) {
        float mixed = getMixedOrientApp(appId, orientation);
        return constrain(MathUtils.norm(this.mMixedOrientationAppMin, this.mMixedOrientationAppMax, mixed));
    }

    public float getMixedOrientApp(int appId, int orientation) {
        if (orientation == 1 || orientation == 3) {
            if (appId != 1 && appId != 2) {
                return 0.0f;
            }
            return 1.0f;
        }
        if (appId != 9) {
            return 0.0f;
        }
        return 2.0f;
    }

    public float antiNormalizeLux(float lux) {
        return MathUtils.constrain(MathUtils.lerp(this.mLuxMin, this.mLuxMax, lux), this.mLuxMin, this.mLuxMax);
    }

    public int antiNormalizeAppId(float appId) {
        return MathUtils.constrain(Math.round(MathUtils.lerp(this.mAppIdMin, this.mAppIdMax, appId)), this.mAppIdMin, this.mAppIdMax);
    }

    public float antiNormalizeBrightness(float brightness) {
        return MathUtils.constrain(MathUtils.lerp(this.mBrightnessMin, this.mBrightnessMax, brightness), this.mBrightnessMin, this.mBrightnessMax);
    }

    private float constrain(float amount) {
        return MathUtils.constrain(amount, 0.0f, 1.0f);
    }
}
