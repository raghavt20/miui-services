package com.android.server.display;

import android.hardware.display.BrightnessConfiguration;
import android.os.SystemProperties;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Spline;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import miui.os.Build;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class BrightnessMappingStrategyImpl implements BrightnessMappingStrategyStub {
    private static final boolean DEBUG = false;
    private static final String TAG = "BrightnessMappingStrategyImpl";
    private BrightnessCurve mBrightnessCurve;
    private float sUnadjustedBrightness = -1.0f;
    private final boolean IS_SUPPORT_AUTOBRIGHTNESS_BY_APPLICATION_CATEGORY = FeatureParser.getBoolean("support_autobrightness_by_application_category", false);
    private final boolean IS_INTERNAL_BUILD = Build.IS_INTERNATIONAL_BUILD;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BrightnessMappingStrategyImpl> {

        /* compiled from: BrightnessMappingStrategyImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BrightnessMappingStrategyImpl INSTANCE = new BrightnessMappingStrategyImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BrightnessMappingStrategyImpl m1004provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BrightnessMappingStrategyImpl m1003provideNewInstance() {
            return new BrightnessMappingStrategyImpl();
        }
    }

    public float getMaxScreenNit() {
        return Float.parseFloat(SystemProperties.get("persist.vendor.max.brightness", "0"));
    }

    public void updateUnadjustedBrightness(float lux, float brightness, float unadjustedbrightness) {
        this.sUnadjustedBrightness = unadjustedbrightness;
    }

    public boolean smoothNewCurve(float[] lux, float[] brightness, int idx) {
        for (int i = idx + 1; i < lux.length; i++) {
            brightness[i] = brightness[i] - (this.sUnadjustedBrightness - brightness[idx]);
            brightness[i] = MathUtils.max(brightness[i], brightness[i - 1]);
        }
        for (int i2 = idx - 1; i2 >= 0; i2--) {
            float f = brightness[i2];
            float f2 = this.sUnadjustedBrightness;
            brightness[i2] = f - (((f2 - brightness[idx]) * brightness[i2]) / f2);
            brightness[i2] = MathUtils.min(brightness[i2], brightness[i2 + 1]);
        }
        return true;
    }

    public boolean isSupportAutobrightnessByApplicationCategory() {
        return this.IS_SUPPORT_AUTOBRIGHTNESS_BY_APPLICATION_CATEGORY && !this.IS_INTERNAL_BUILD;
    }

    public BrightnessMappingStrategy getMiuiMapperInstance(BrightnessConfiguration build, float[] nitsRange, float[] brightnessRange, float autoBrightnessAdjustmentMaxGamma) {
        return new MiuiPhysicalBrightnessMappingStrategy(build, nitsRange, brightnessRange, autoBrightnessAdjustmentMaxGamma);
    }

    public Pair<float[], float[]> smoothNewCurveV2(float[] lux, float[] brightness, int idx, boolean curveV2Enable) {
        BrightnessCurve brightnessCurve;
        if (curveV2Enable && (brightnessCurve = this.mBrightnessCurve) != null && brightnessCurve.isEnable()) {
            return this.mBrightnessCurve.smoothNewCurveV2(lux, brightness, idx);
        }
        smoothNewCurve(lux, brightness, idx);
        return Pair.create(lux, brightness);
    }

    public void initMiuiCurve(BrightnessConfiguration defaultConfig, Spline nitsToBrightnessSpline, Spline brightnessToNitsSpline) {
        this.mBrightnessCurve = new BrightnessCurve(defaultConfig, nitsToBrightnessSpline, brightnessToNitsSpline);
    }

    public void updateSplineConfig(BrightnessConfiguration config, Spline nitsToBrightnessSpline, Spline brightnessToNitsSpline) {
        this.mBrightnessCurve.updateSplineConfig(config, nitsToBrightnessSpline, brightnessToNitsSpline);
    }

    public void dump(PrintWriter pw) {
        BrightnessCurve brightnessCurve = this.mBrightnessCurve;
        if (brightnessCurve != null) {
            brightnessCurve.dump(pw);
        }
    }
}
