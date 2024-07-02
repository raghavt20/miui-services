package com.android.server.display;

import android.content.res.Resources;
import android.hardware.display.BrightnessConfiguration;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Spline;
import com.android.server.display.aiautobrt.CustomBrightnessModeController;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BrightnessCurve {
    private static final int DARK_LIGHT_CURVE_HEAD_LUX_INDEX = 0;
    private static final float DARK_LIGHT_CURVE_RADIO = 0.7f;
    private static final int DARK_LIGHT_CURVE_TAIL_LUX_INDEX = 1;
    private static final int FIRST_OUTDOOR_LIGHT_CURVE_TAIL_INDEX = 3;
    private static final int INDOOR_LIGHT_CURVE_TAIL_LUX_INDEX = 2;
    private static final int SECOND_OUTDOOR_LIGHT_CURVE_TAIL_INDEX = 4;
    private static final String TAG = "BrightnessCurve";
    private float[] mBrightness;
    private final boolean mBrightnessCurveAlgoAvailable;
    private final float mBrightnessMinTan;
    private Spline mBrightnessToNit;
    private BrightnessConfiguration mConfig;
    private float[] mCurrentCurveInterval;
    private final float mCurveAnchorPointLux;
    private final float[] mCustomBrighteningCurveInterval;
    private final float[] mCustomDarkeningCurveInterval;
    private final float mDarkLightCurvePullUpLimitNit;
    private final float mDarkLightCurvePullUpMaxTan;
    private BrightnessConfiguration mDefault;
    private final float[] mDefaultCurveInterval;
    private final float mIndoorLightCurveTan;
    private float[] mLux;
    private Spline mLuxToNit;
    private Spline mLuxToNitsDefault;
    private float mMaxLux;
    private float mMinNit;
    private Spline mNitToBrightness;
    private final float mSecondOutdoorCurveBrightenMinTan;
    private final float mThirdOutdoorCurveBrightenMinTan;

    public BrightnessCurve(BrightnessConfiguration defaultConfig, Spline nitsToBrightnessSpline, Spline brightnessToNitsSpline) {
        this.mConfig = defaultConfig;
        this.mDefault = defaultConfig;
        this.mNitToBrightness = nitsToBrightnessSpline;
        this.mBrightnessToNit = brightnessToNitsSpline;
        Resources resources = Resources.getSystem();
        float[] floatArray = BrightnessMappingStrategy.getFloatArray(resources.obtainTypedArray(285409340));
        this.mDefaultCurveInterval = floatArray;
        this.mCustomBrighteningCurveInterval = BrightnessMappingStrategy.getFloatArray(resources.obtainTypedArray(285409335));
        this.mCustomDarkeningCurveInterval = BrightnessMappingStrategy.getFloatArray(resources.obtainTypedArray(285409336));
        this.mBrightnessMinTan = resources.getFloat(285671462);
        this.mSecondOutdoorCurveBrightenMinTan = resources.getFloat(285671475);
        this.mThirdOutdoorCurveBrightenMinTan = resources.getFloat(285671481);
        this.mCurveAnchorPointLux = resources.getFloat(285671463);
        this.mDarkLightCurvePullUpMaxTan = resources.getFloat(285671465);
        this.mDarkLightCurvePullUpLimitNit = resources.getFloat(285671464);
        this.mBrightnessCurveAlgoAvailable = resources.getBoolean(285540366);
        this.mIndoorLightCurveTan = resources.getFloat(285671468);
        this.mCurrentCurveInterval = floatArray;
    }

    public void updateSplineConfig(BrightnessConfiguration config, Spline nitsToBrightnessSpline, Spline brightnessToNitsSpline) {
        this.mConfig = config;
        this.mNitToBrightness = nitsToBrightnessSpline;
        this.mBrightnessToNit = brightnessToNitsSpline;
    }

    private void updateData(float[] lux, float[] brightness) {
        setCurveInterval(this.mConfig);
        Pair<float[], float[]> defaultCurve = this.mConfig.getCurve();
        this.mLuxToNitsDefault = Spline.createLinearSpline((float[]) defaultCurve.first, (float[]) defaultCurve.second);
        this.mMinNit = this.mBrightnessToNit.interpolate(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        this.mMaxLux = ((float[]) defaultCurve.first)[((float[]) defaultCurve.first).length - 1];
        this.mLux = lux;
        this.mBrightness = brightness;
    }

    private void setCurveInterval(BrightnessConfiguration config) {
        String description = config.getDescription();
        if (CustomBrightnessModeController.CUSTOM_BRIGHTNESS_CURVE_BRIGHTENING.equals(description)) {
            this.mCurrentCurveInterval = this.mCustomBrighteningCurveInterval;
        } else if (CustomBrightnessModeController.CUSTOM_BRIGHTNESS_CURVE_DARKENING.equals(description)) {
            this.mCurrentCurveInterval = this.mCustomDarkeningCurveInterval;
        } else {
            this.mCurrentCurveInterval = this.mDefaultCurveInterval;
        }
    }

    public Pair<float[], float[]> smoothNewCurveV2(float[] lux, float[] brightness, int idx) {
        float changeLux = lux[idx];
        float changeNit = this.mBrightnessToNit.interpolate(brightness[idx]);
        Slog.d(TAG, "smoothNewCurveV2: changeLux: " + changeLux + ", changeNit: " + changeNit);
        updateData(lux, brightness);
        List<Curve> listPoint = buildCurve(changeLux, changeNit);
        createSpline(listPoint);
        computeBrightness();
        return Pair.create(this.mLux, this.mBrightness);
    }

    private void createSpline(List<Curve> list) {
        List<Pair<Float, Float>> splinePointList = getSplinePointList(list);
        float[] lux = new float[splinePointList.size()];
        float[] nit = new float[splinePointList.size()];
        for (int i = 0; i < splinePointList.size(); i++) {
            Pair<Float, Float> point = splinePointList.get(i);
            lux[i] = ((Float) point.first).floatValue();
            nit[i] = ((Float) point.second).floatValue();
        }
        this.mLuxToNit = Spline.createLinearSpline(lux, nit);
    }

    private List<Pair<Float, Float>> getSplinePointList(List<Curve> list) {
        List<Pair<Float, Float>> splinePointList = new ArrayList<>();
        if (list.size() > 0 && list.get(0).mPointList.size() > 0) {
            splinePointList.add(list.get(0).mPointList.get(0));
            for (Curve curve : list) {
                for (int i = 1; i < curve.mPointList.size(); i++) {
                    splinePointList.add(curve.mPointList.get(i));
                }
            }
        }
        return splinePointList;
    }

    private void computeBrightness() {
        int i = 0;
        while (true) {
            float[] fArr = this.mLux;
            if (i < fArr.length && i < this.mBrightness.length) {
                float nit = this.mLuxToNit.interpolate(fArr[i]);
                if (nit < this.mMinNit) {
                    nit = this.mMinNit;
                }
                this.mBrightness[i] = this.mNitToBrightness.interpolate(nit);
                if (i != 0) {
                    float[] fArr2 = this.mBrightness;
                    fArr2[i] = MathUtils.max(fArr2[i], fArr2[i - 1]);
                }
                i++;
            } else {
                return;
            }
        }
    }

    private List<Curve> buildCurve(float lux, float changeNit) {
        Curve darkLightCurve = new DarkLightCurve();
        Curve indoorLightCurve = new IndoorLightCurve();
        Curve firstOutdoorLightCurve = new FirstOutdoorLightCurve();
        Curve secondOutdoorLightCurve = new SecondOutdoorLightCurve();
        Curve thirdOutdoorLightCurve = new ThirdOutdoorLightCurve();
        List<Curve> curveList = new ArrayList<>();
        float[] fArr = this.mCurrentCurveInterval;
        if (lux >= fArr[0] && lux < fArr[1]) {
            darkLightCurve.create(lux, changeNit);
            indoorLightCurve.connectLeft(darkLightCurve);
            firstOutdoorLightCurve.connectLeft(indoorLightCurve);
            secondOutdoorLightCurve.connectLeft(firstOutdoorLightCurve);
            thirdOutdoorLightCurve.connectLeft(secondOutdoorLightCurve);
        } else if (lux >= fArr[1] && lux < fArr[2]) {
            indoorLightCurve.create(lux, changeNit);
            darkLightCurve.connectRight(indoorLightCurve);
            firstOutdoorLightCurve.connectLeft(indoorLightCurve);
            secondOutdoorLightCurve.connectLeft(firstOutdoorLightCurve);
            thirdOutdoorLightCurve.connectLeft(secondOutdoorLightCurve);
        } else if (lux >= fArr[2] && lux < fArr[3]) {
            firstOutdoorLightCurve.create(lux, changeNit);
            indoorLightCurve.connectRight(firstOutdoorLightCurve);
            darkLightCurve.connectRight(indoorLightCurve);
            secondOutdoorLightCurve.connectLeft(firstOutdoorLightCurve);
            thirdOutdoorLightCurve.connectLeft(secondOutdoorLightCurve);
        } else if (lux >= fArr[3] && lux < fArr[4]) {
            secondOutdoorLightCurve.create(lux, changeNit);
            firstOutdoorLightCurve.connectRight(secondOutdoorLightCurve);
            indoorLightCurve.connectRight(firstOutdoorLightCurve);
            darkLightCurve.connectRight(indoorLightCurve);
            thirdOutdoorLightCurve.connectLeft(secondOutdoorLightCurve);
        } else if (lux >= fArr[4]) {
            thirdOutdoorLightCurve.create(lux, changeNit);
            secondOutdoorLightCurve.connectRight(thirdOutdoorLightCurve);
            firstOutdoorLightCurve.connectRight(secondOutdoorLightCurve);
            indoorLightCurve.connectRight(firstOutdoorLightCurve);
            darkLightCurve.connectRight(indoorLightCurve);
        }
        curveList.add(darkLightCurve);
        curveList.add(indoorLightCurve);
        curveList.add(firstOutdoorLightCurve);
        curveList.add(secondOutdoorLightCurve);
        curveList.add(thirdOutdoorLightCurve);
        return curveList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void copyToDefaultSpline(float startLux, float endLux, List<Pair<Float, Float>> mPointList, float diffNit, float ratio) {
        int i = 0;
        while (true) {
            float[] fArr = this.mLux;
            if (i >= fArr.length) {
                return;
            }
            float f = fArr[i];
            if (f <= endLux) {
                if (f >= startLux) {
                    float coefficient = 1.0f - (((endLux - f) * ratio) / (endLux - startLux));
                    Pair<Float, Float> point = new Pair<>(Float.valueOf(this.mLux[i]), Float.valueOf(this.mLuxToNitsDefault.interpolate(this.mLux[i]) - (diffNit * coefficient)));
                    mPointList.add(point);
                }
                i++;
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pullDownCurveCreate(float lux, float changeNit, List<Pair<Float, Float>> pointList, float headLux, float tailLux, float minTanToHead, float minTanToTail) {
        float tan0LuxToTailLux = (this.mLuxToNitsDefault.interpolate(tailLux) - this.mLuxToNitsDefault.interpolate(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)) / tailLux;
        float tanToHeadLux = minTanToHead;
        if (lux != headLux) {
            tanToHeadLux = (changeNit - this.mLuxToNitsDefault.interpolate(headLux)) / (lux - headLux);
        }
        if (tanToHeadLux < minTanToHead) {
            tanToHeadLux = minTanToHead;
        }
        float headPointNit = changeNit - ((lux - headLux) * tanToHeadLux);
        Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(headLux), Float.valueOf(headPointNit));
        pointList.add(headPoint);
        if (lux != headLux) {
            Pair<Float, Float> changePoint = new Pair<>(Float.valueOf(lux), Float.valueOf(changeNit));
            pointList.add(changePoint);
        }
        float tanToStartPoint = (changeNit - this.mLuxToNitsDefault.interpolate(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)) / lux;
        if (tanToStartPoint > tan0LuxToTailLux) {
            Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(tailLux), Float.valueOf(this.mLuxToNitsDefault.interpolate(tailLux)));
            pointList.add(tailPoint);
            return;
        }
        if (tanToStartPoint < minTanToTail) {
            tanToStartPoint = minTanToTail;
        }
        float tailPointNit = ((tailLux - lux) * tanToStartPoint) + changeNit;
        Pair<Float, Float> tailPoint2 = new Pair<>(Float.valueOf(tailLux), Float.valueOf(tailPointNit));
        pointList.add(tailPoint2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pullUpCurveCreate(float lux, float changeNit, List<Pair<Float, Float>> pointList, float headLux, float tailLux, float anchorPointLux, float anchorPointNit) {
        float tanToAnchor = (changeNit - anchorPointNit) / (lux - anchorPointLux);
        float headPointNit = changeNit - ((lux - headLux) * tanToAnchor);
        Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(headLux), Float.valueOf(headPointNit));
        pointList.add(headPoint);
        float tailPointNit = ((tailLux - lux) * tanToAnchor) + changeNit;
        Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(tailLux), Float.valueOf(tailPointNit));
        pointList.add(tailPoint);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pullUpConnectTail(Pair<Float, Float> headPoint, float tailLux, float minTan, List<Pair<Float, Float>> pointList) {
        pointList.add(headPoint);
        float lux = ((Float) headPoint.first).floatValue();
        float changeNit = ((Float) headPoint.second).floatValue();
        float tanToTailPoint = minTan;
        if (tailLux != lux) {
            tanToTailPoint = (this.mLuxToNitsDefault.interpolate(tailLux) - changeNit) / (tailLux - lux);
        }
        if (tanToTailPoint < minTan) {
            tanToTailPoint = minTan;
        }
        float tailPointNit = ((tailLux - lux) * tanToTailPoint) + changeNit;
        Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(tailLux), Float.valueOf(tailPointNit));
        pointList.add(tailPoint);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public abstract class Curve {
        public List<Pair<Float, Float>> mPointList = new ArrayList();

        public abstract void connectLeft(Curve curve);

        public abstract void connectRight(Curve curve);

        public abstract void create(float f, float f2);

        public Curve() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DarkLightCurve extends Curve {
        private final float mHeadLux;
        private final float mPullUpLimitNit;
        private final float mPullUpMaxTan;
        private final float mTailLux;

        public DarkLightCurve() {
            super();
            this.mHeadLux = BrightnessCurve.this.mCurrentCurveInterval[0];
            this.mTailLux = BrightnessCurve.this.mCurrentCurveInterval[1];
            this.mPullUpMaxTan = BrightnessCurve.this.mDarkLightCurvePullUpMaxTan;
            this.mPullUpLimitNit = BrightnessCurve.this.mDarkLightCurvePullUpLimitNit;
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void create(float lux, float changeNit) {
            if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux) > changeNit) {
                if (lux >= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && lux <= 3.0f) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux) - changeNit, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                    return;
                } else {
                    BrightnessCurve.this.pullDownCurveCreate(lux, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, BrightnessCurve.this.mBrightnessMinTan, BrightnessCurve.this.mBrightnessMinTan);
                    return;
                }
            }
            float diffNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux) - changeNit;
            float tailPointNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mTailLux) - diffNit;
            float f = this.mPullUpLimitNit;
            float f2 = this.mTailLux;
            float tanToLimitPoint = (f - changeNit) / (f2 - lux);
            if (tailPointNit < f) {
                BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, f2, this.mPointList, diffNit, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                return;
            }
            if (tanToLimitPoint < this.mPullUpMaxTan) {
                tanToLimitPoint = this.mPullUpMaxTan;
            }
            float tailPointNit2 = changeNit + ((f2 - lux) * tanToLimitPoint);
            float headPointNit = changeNit - ((lux - this.mHeadLux) * tanToLimitPoint);
            Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(this.mHeadLux), Float.valueOf(headPointNit));
            this.mPointList.add(headPoint);
            Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(this.mTailLux), Float.valueOf(tailPointNit2));
            this.mPointList.add(tailPoint);
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectLeft(Curve curve) {
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectRight(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> tailPoint = curve.mPointList.get(0);
                if (((Float) tailPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mTailLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) tailPoint.first).floatValue()) > ((Float) tailPoint.second).floatValue()) {
                    create(((Float) tailPoint.first).floatValue(), ((Float) tailPoint.second).floatValue());
                } else {
                    float diffNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) tailPoint.first).floatValue()) - ((Float) tailPoint.second).floatValue();
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, diffNit, BrightnessCurve.DARK_LIGHT_CURVE_RADIO);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class IndoorLightCurve extends Curve {
        private final float mAnchorPointLux;
        private final float mHeadLux;
        private final float mTailLux;

        public IndoorLightCurve() {
            super();
            this.mHeadLux = BrightnessCurve.this.mCurrentCurveInterval[1];
            this.mTailLux = BrightnessCurve.this.mCurrentCurveInterval[2];
            this.mAnchorPointLux = BrightnessCurve.this.mCurveAnchorPointLux;
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void create(float lux, float changeNit) {
            float tanToAnchorPoint = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mAnchorPointLux) - changeNit) / (this.mAnchorPointLux - lux);
            if (tanToAnchorPoint < BrightnessCurve.this.mBrightnessMinTan) {
                tanToAnchorPoint = BrightnessCurve.this.mBrightnessMinTan;
            }
            float headPointNit = changeNit - ((lux - this.mHeadLux) * BrightnessCurve.this.mIndoorLightCurveTan);
            Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(this.mHeadLux), Float.valueOf(headPointNit));
            this.mPointList.add(headPoint);
            Pair<Float, Float> changePoint = new Pair<>(Float.valueOf(lux), Float.valueOf(changeNit));
            this.mPointList.add(changePoint);
            float tailPointNit = ((this.mTailLux - lux) * tanToAnchorPoint) + changeNit;
            Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(this.mTailLux), Float.valueOf(tailPointNit));
            this.mPointList.add(tailPoint);
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectLeft(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> headPoint = curve.mPointList.get(curve.mPointList.size() - 1);
                if (((Float) headPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) headPoint.first).floatValue()) > ((Float) headPoint.second).floatValue()) {
                    pullDownConnectLeft(headPoint);
                } else {
                    pullUpConnectLeft(headPoint);
                }
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectRight(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> tailPoint = curve.mPointList.get(0);
                if (((Float) tailPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mTailLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                    return;
                }
                if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) tailPoint.first).floatValue()) > ((Float) tailPoint.second).floatValue()) {
                    pullDownConnectRight(tailPoint);
                    return;
                }
                float headPointNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux);
                Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(this.mHeadLux), Float.valueOf(headPointNit));
                this.mPointList.add(headPoint);
                this.mPointList.add(tailPoint);
            }
        }

        private void pullUpConnectLeft(Pair<Float, Float> headPoint) {
            this.mPointList.add(headPoint);
            float tanToAnchorPoint = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mAnchorPointLux) - ((Float) headPoint.second).floatValue()) / (this.mAnchorPointLux - ((Float) headPoint.first).floatValue());
            if (tanToAnchorPoint < BrightnessCurve.this.mBrightnessMinTan) {
                tanToAnchorPoint = BrightnessCurve.this.mBrightnessMinTan;
            }
            float tailPointNit = ((Float) headPoint.second).floatValue() + ((this.mTailLux - ((Float) headPoint.first).floatValue()) * tanToAnchorPoint);
            Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(this.mTailLux), Float.valueOf(tailPointNit));
            this.mPointList.add(tailPoint);
        }

        private void pullDownConnectLeft(Pair<Float, Float> headPoint) {
            this.mPointList.add(headPoint);
            float tanToAnchorPoint = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mAnchorPointLux) - ((Float) headPoint.second).floatValue()) / (this.mAnchorPointLux - ((Float) headPoint.first).floatValue());
            float tailPointNit = ((Float) headPoint.second).floatValue() + ((this.mTailLux - ((Float) headPoint.first).floatValue()) * tanToAnchorPoint);
            Pair<Float, Float> tailPoint = new Pair<>(Float.valueOf(this.mTailLux), Float.valueOf(tailPointNit));
            this.mPointList.add(tailPoint);
        }

        private void pullDownConnectRight(Pair<Float, Float> tailPoint) {
            float tanToHead = (((Float) tailPoint.second).floatValue() - BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux)) / (this.mTailLux - this.mHeadLux);
            if (tanToHead < BrightnessCurve.this.mBrightnessMinTan) {
                tanToHead = BrightnessCurve.this.mBrightnessMinTan;
            }
            float headPointNit = ((Float) tailPoint.second).floatValue() - ((((Float) tailPoint.first).floatValue() - this.mHeadLux) * tanToHead);
            Pair<Float, Float> headPoint = new Pair<>(Float.valueOf(this.mHeadLux), Float.valueOf(headPointNit));
            this.mPointList.add(headPoint);
            this.mPointList.add(tailPoint);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class FirstOutdoorLightCurve extends Curve {
        private final float mAnchorPointLux;
        private final float mHeadLux;
        private final float mPullUpAnchorPointLux;
        private final float mPullUpAnchorPointNit;
        private final float mTailLux;

        public FirstOutdoorLightCurve() {
            super();
            float f = BrightnessCurve.this.mCurrentCurveInterval[2];
            this.mHeadLux = f;
            float f2 = BrightnessCurve.this.mCurrentCurveInterval[3];
            this.mTailLux = f2;
            this.mAnchorPointLux = BrightnessCurve.this.mCurveAnchorPointLux;
            float f3 = BrightnessCurve.this.mCurrentCurveInterval[1];
            this.mPullUpAnchorPointLux = f3;
            float tan = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - BrightnessCurve.this.mLuxToNitsDefault.interpolate(f)) / (f2 - f);
            this.mPullUpAnchorPointNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - ((f2 - f3) * tan);
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void create(float lux, float changeNit) {
            if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux) > changeNit) {
                BrightnessCurve.this.pullDownCurveCreate(lux, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, BrightnessCurve.this.mBrightnessMinTan, BrightnessCurve.this.mBrightnessMinTan);
            } else {
                BrightnessCurve.this.pullUpCurveCreate(lux, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, this.mPullUpAnchorPointLux, this.mPullUpAnchorPointNit);
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectLeft(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> headPoint = curve.mPointList.get(curve.mPointList.size() - 1);
                if (((Float) headPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) headPoint.first).floatValue()) > ((Float) headPoint.second).floatValue()) {
                    this.mPointList.add(headPoint);
                    BrightnessCurve.this.copyToDefaultSpline(this.mAnchorPointLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else {
                    pullUpConnectLeft(headPoint);
                }
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectRight(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> tailPoint = curve.mPointList.get(0);
                if (((Float) tailPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mTailLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else {
                    create(((Float) tailPoint.first).floatValue(), ((Float) tailPoint.second).floatValue());
                }
            }
        }

        private void pullUpConnectLeft(Pair<Float, Float> headPoint) {
            this.mPointList.add(headPoint);
            float tanToAnchorPoint = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mAnchorPointLux) - ((Float) headPoint.second).floatValue()) / (this.mAnchorPointLux - ((Float) headPoint.first).floatValue());
            if (tanToAnchorPoint > BrightnessCurve.this.mBrightnessMinTan) {
                BrightnessCurve.this.copyToDefaultSpline(this.mAnchorPointLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                return;
            }
            float tanToAnchorPoint2 = BrightnessCurve.this.mBrightnessMinTan;
            float anchorPointNit = ((Float) headPoint.second).floatValue() + ((this.mAnchorPointLux - ((Float) headPoint.first).floatValue()) * tanToAnchorPoint2);
            Pair<Float, Float> anchorPoint = new Pair<>(Float.valueOf(this.mAnchorPointLux), Float.valueOf(anchorPointNit));
            BrightnessCurve brightnessCurve = BrightnessCurve.this;
            brightnessCurve.pullUpConnectTail(anchorPoint, this.mTailLux, brightnessCurve.mBrightnessMinTan, this.mPointList);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SecondOutdoorLightCurve extends Curve {
        private final float mHeadLux;
        private final float mPullUpAnchorPointLux;
        private final float mPullUpAnchorPointNit;
        private final float mTailLux;

        public SecondOutdoorLightCurve() {
            super();
            float f = BrightnessCurve.this.mCurrentCurveInterval[3];
            this.mHeadLux = f;
            float f2 = BrightnessCurve.this.mCurrentCurveInterval[4];
            this.mTailLux = f2;
            float f3 = BrightnessCurve.this.mCurrentCurveInterval[1];
            this.mPullUpAnchorPointLux = f3;
            float tan = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - BrightnessCurve.this.mLuxToNitsDefault.interpolate(f)) / (f2 - f);
            this.mPullUpAnchorPointNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - ((f2 - f3) * tan);
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void create(float lux, float changeNit) {
            if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux) > changeNit) {
                BrightnessCurve.this.pullDownCurveCreate(lux, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, BrightnessCurve.this.mSecondOutdoorCurveBrightenMinTan, BrightnessCurve.this.mSecondOutdoorCurveBrightenMinTan);
            } else {
                BrightnessCurve.this.pullUpCurveCreate(lux, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, this.mPullUpAnchorPointLux, this.mPullUpAnchorPointNit);
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectLeft(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> headPoint = curve.mPointList.get(curve.mPointList.size() - 1);
                if (((Float) headPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) headPoint.first).floatValue()) > ((Float) headPoint.second).floatValue()) {
                    create(((Float) headPoint.first).floatValue(), ((Float) headPoint.second).floatValue());
                } else {
                    BrightnessCurve brightnessCurve = BrightnessCurve.this;
                    brightnessCurve.pullUpConnectTail(headPoint, this.mTailLux, brightnessCurve.mSecondOutdoorCurveBrightenMinTan, this.mPointList);
                }
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectRight(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> tailPoint = curve.mPointList.get(0);
                if (((Float) tailPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mTailLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else {
                    create(((Float) tailPoint.first).floatValue(), ((Float) tailPoint.second).floatValue());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ThirdOutdoorLightCurve extends Curve {
        private final float mHeadLux;
        private final float mPullUpAnchorPointLux;
        private final float mPullUpAnchorPointNit;
        private final float mTailLux;

        public ThirdOutdoorLightCurve() {
            super();
            float f = BrightnessCurve.this.mCurrentCurveInterval[4];
            this.mHeadLux = f;
            float f2 = BrightnessCurve.this.mMaxLux > f ? BrightnessCurve.this.mMaxLux : 1.0f + f;
            this.mTailLux = f2;
            float f3 = BrightnessCurve.this.mCurrentCurveInterval[1];
            this.mPullUpAnchorPointLux = f3;
            float tan = (BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - BrightnessCurve.this.mLuxToNitsDefault.interpolate(f)) / (f2 - f);
            this.mPullUpAnchorPointNit = BrightnessCurve.this.mLuxToNitsDefault.interpolate(f2) - ((f2 - f3) * tan);
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void create(float lux, float changeNit) {
            float lux2 = lux > BrightnessCurve.this.mMaxLux ? BrightnessCurve.this.mMaxLux : lux;
            if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(lux2) > changeNit) {
                BrightnessCurve.this.pullDownCurveCreate(lux2, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, BrightnessCurve.this.mThirdOutdoorCurveBrightenMinTan, BrightnessCurve.this.mThirdOutdoorCurveBrightenMinTan);
            } else {
                BrightnessCurve.this.pullUpCurveCreate(lux2, changeNit, this.mPointList, this.mHeadLux, this.mTailLux, this.mPullUpAnchorPointLux, this.mPullUpAnchorPointNit);
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectLeft(Curve curve) {
            if (curve.mPointList.size() != 0) {
                Pair<Float, Float> headPoint = curve.mPointList.get(curve.mPointList.size() - 1);
                if (((Float) headPoint.second).floatValue() == BrightnessCurve.this.mLuxToNitsDefault.interpolate(this.mHeadLux)) {
                    BrightnessCurve.this.copyToDefaultSpline(this.mHeadLux, this.mTailLux, this.mPointList, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                } else if (BrightnessCurve.this.mLuxToNitsDefault.interpolate(((Float) headPoint.first).floatValue()) > ((Float) headPoint.second).floatValue()) {
                    create(((Float) headPoint.first).floatValue(), ((Float) headPoint.second).floatValue());
                } else {
                    BrightnessCurve brightnessCurve = BrightnessCurve.this;
                    brightnessCurve.pullUpConnectTail(headPoint, this.mTailLux, brightnessCurve.mThirdOutdoorCurveBrightenMinTan, this.mPointList);
                }
            }
        }

        @Override // com.android.server.display.BrightnessCurve.Curve
        public void connectRight(Curve curve) {
        }
    }

    public boolean isEnable() {
        String description = this.mConfig.getDescription();
        if (this.mBrightnessCurveAlgoAvailable) {
            if (CustomBrightnessModeController.CUSTOM_BRIGHTNESS_CURVE_DEFAULT.equals(description) || CustomBrightnessModeController.CUSTOM_BRIGHTNESS_CURVE_BRIGHTENING.equals(description) || CustomBrightnessModeController.CUSTOM_BRIGHTNESS_CURVE_DARKENING.equals(description) || this.mDefault.equals(this.mConfig)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("BrightnessCurve Configuration:");
        pw.println("  mBrightnessCurveAlgoAvailable=" + this.mBrightnessCurveAlgoAvailable);
        pw.println("  mConfig=" + this.mConfig);
    }
}
