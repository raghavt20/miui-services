package com.android.server.display;

import android.content.res.Resources;
import android.util.Slog;
import android.util.Spline;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class HysteresisLevelsImpl extends HysteresisLevelsStub {
    private static final float HBM_MINIMUM_LUX = 6000.0f;
    private static final String TAG = "HysteresisLevelsImpl";
    private HysteresisLevels mAmbientBrightnessThresholds;
    private HighBrightnessModeController mHbmController;
    private Spline mHysteresisBrightSpline;
    private Spline mHysteresisDarkSpline;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<HysteresisLevelsImpl> {

        /* compiled from: HysteresisLevelsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final HysteresisLevelsImpl INSTANCE = new HysteresisLevelsImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public HysteresisLevelsImpl m1112provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public HysteresisLevelsImpl m1111provideNewInstance() {
            return new HysteresisLevelsImpl();
        }
    }

    public void initialize(HighBrightnessModeController hbmController, HysteresisLevels ambientBrightnessThresholds) {
        this.mHbmController = hbmController;
        this.mAmbientBrightnessThresholds = ambientBrightnessThresholds;
        float[] ambientBrighteningLux = BrightnessMappingStrategy.getFloatArray(Resources.getSystem().obtainTypedArray(285409310));
        float[] ambientBrighteningThresholds = BrightnessMappingStrategy.getFloatArray(Resources.getSystem().obtainTypedArray(285409315));
        float[] ambientDarkeningLux = BrightnessMappingStrategy.getFloatArray(Resources.getSystem().obtainTypedArray(285409320));
        float[] ambientDarkeningThresholds = BrightnessMappingStrategy.getFloatArray(Resources.getSystem().obtainTypedArray(285409325));
        createHysteresisThresholdSpline(ambientBrighteningLux, ambientBrighteningThresholds, ambientDarkeningLux, ambientDarkeningThresholds);
    }

    public void createHysteresisThresholdSpline(float[] ambientBrighteningLux, float[] ambientBrighteningThresholds, float[] ambientDarkeningLux, float[] ambientDarkeningThresholds) {
        if (ambientBrighteningLux.length != ambientBrighteningThresholds.length || ambientDarkeningLux.length != ambientDarkeningThresholds.length) {
            Slog.e(TAG, "Mismatch between hysteresis array lengths.");
            return;
        }
        if (ambientBrighteningLux.length > 0 && ambientBrighteningThresholds.length > 0 && ambientDarkeningLux.length > 0 && ambientDarkeningThresholds.length > 0) {
            this.mHysteresisBrightSpline = Spline.createLinearSpline(ambientBrighteningLux, ambientBrighteningThresholds);
            this.mHysteresisDarkSpline = Spline.createLinearSpline(ambientDarkeningLux, ambientDarkeningThresholds);
        }
    }

    public float getBrighteningThreshold(float value) {
        Spline spline = this.mHysteresisBrightSpline;
        if (spline != null) {
            float brighteningThreshold = spline.interpolate(value);
            float hbmMinValue = HBM_MINIMUM_LUX;
            HighBrightnessModeController highBrightnessModeController = this.mHbmController;
            if (highBrightnessModeController != null && highBrightnessModeController.deviceSupportsHbm()) {
                hbmMinValue = this.mHbmController.getHbmData().minimumLux;
            }
            return value > hbmMinValue ? 1.0f + value : brighteningThreshold;
        }
        return this.mAmbientBrightnessThresholds.getBrighteningThreshold(value);
    }

    public float getDarkeningThreshold(float value) {
        Spline spline = this.mHysteresisDarkSpline;
        if (spline != null) {
            return spline.interpolate(value);
        }
        return this.mAmbientBrightnessThresholds.getDarkeningThreshold(value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("MiuiHysteresisLevels:");
        pw.println("  mHysteresisBrightSpline=" + this.mHysteresisBrightSpline);
        pw.println("  mHysteresisDarkSpline=" + this.mHysteresisDarkSpline);
    }
}
