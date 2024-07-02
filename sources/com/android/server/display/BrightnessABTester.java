package com.android.server.display;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.display.statistics.BrightnessDataProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class BrightnessABTester {
    private static final String APP_NAME = "AutoBrightness";
    private static final String CONTROL_GROUP = "control_group";
    private static final long DELAY_TIME = 30000;
    private static final String EXPERIMENTAL_GROUP_1 = "experimental_group_1";
    private static final String EXPERIMENTAL_GROUP_2 = "experimental_group_2";
    private static final String EXPERIMENTAL_GROUP_3 = "experimental_group_3";
    private static final String EXPERIMENTAL_GROUP_4 = "experimental_group_4";
    private static String LAYER_NAME = SystemProperties.get("ro.product.mod_device", "");
    private static final String TAG = "BrightnessABTester";
    private static final String THRESHOLD_EXP_KEY = "threshold_type";
    private ABTestHelper mABTestHelper;
    private float[] mAmbientBrighteningLux;
    private float[] mAmbientBrighteningThresholds;
    private float[] mAmbientDarkeningLux;
    private float[] mAmbientDarkeningThresholds;
    private AutomaticBrightnessControllerImpl mAutomaticBrightnessControllerImpl;
    private BrightnessDataProcessor mBrightnessDataProcessor;
    private String mCurrentExperiment = "";

    public BrightnessABTester(Looper looper, Context context, BrightnessDataProcessor brightnessDataProcessor) {
        this.mBrightnessDataProcessor = brightnessDataProcessor;
        Map<String, String> expCondition = new HashMap<>();
        this.mABTestHelper = new ABTestHelper(looper, context, APP_NAME, LAYER_NAME, expCondition, new Consumer() { // from class: com.android.server.display.BrightnessABTester$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BrightnessABTester.this.startExperiment((Map) obj);
            }
        }, new Consumer() { // from class: com.android.server.display.BrightnessABTester$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BrightnessABTester.this.endExperiment((Void) obj);
            }
        }, 30000L);
    }

    public void setAutomaticBrightnessControllerImpl(AutomaticBrightnessControllerImpl stub) {
        this.mAutomaticBrightnessControllerImpl = stub;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startExperiment(Map<String, String> expParams) {
        expParams.forEach(new BiConsumer() { // from class: com.android.server.display.BrightnessABTester$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                BrightnessABTester.this.lambda$startExperiment$0((String) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startExperiment$0(String key, String value) {
        char c;
        switch (key.hashCode()) {
            case 1840541326:
                if (key.equals(THRESHOLD_EXP_KEY)) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                updateBrightnessThresholdABTest(value);
                this.mCurrentExperiment = key;
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void endExperiment(Void unused) {
        char c;
        String str = this.mCurrentExperiment;
        switch (str.hashCode()) {
            case 1840541326:
                if (str.equals(THRESHOLD_EXP_KEY)) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                updateBrightnessThresholdABTest(CONTROL_GROUP);
                this.mAutomaticBrightnessControllerImpl.setAnimationPolicyDisable(false);
                break;
        }
        this.mCurrentExperiment = "";
        this.mBrightnessDataProcessor.setExpId(this.mABTestHelper.getExpId());
    }

    private void getValuesFromXml(int configAmbientBrighteningLux, int configAmbientBrighteningThresholds, int configAmbientDarkeningLux, int configAmbientDarkeningThresholds) {
        this.mAmbientBrighteningLux = getFloatArray(Resources.getSystem().obtainTypedArray(configAmbientBrighteningLux));
        this.mAmbientBrighteningThresholds = getFloatArray(Resources.getSystem().obtainTypedArray(configAmbientBrighteningThresholds));
        this.mAmbientDarkeningLux = getFloatArray(Resources.getSystem().obtainTypedArray(configAmbientDarkeningLux));
        this.mAmbientDarkeningThresholds = getFloatArray(Resources.getSystem().obtainTypedArray(configAmbientDarkeningThresholds));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void updateBrightnessThresholdABTest(String value) {
        char c;
        HysteresisLevelsStub hysteresisLevelsImpl;
        Slog.i(TAG, "The abtest experiment of brightness target " + value);
        switch (value.hashCode()) {
            case -1319601638:
                if (value.equals(EXPERIMENTAL_GROUP_1)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1319601637:
                if (value.equals(EXPERIMENTAL_GROUP_2)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1319601636:
                if (value.equals(EXPERIMENTAL_GROUP_3)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1319601635:
                if (value.equals(EXPERIMENTAL_GROUP_4)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1215857949:
                if (value.equals(CONTROL_GROUP)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                getValuesFromXml(285409312, 285409317, 285409322, 285409327);
                break;
            case 1:
                getValuesFromXml(285409314, 285409319, 285409324, 285409329);
                break;
            case 2:
                getValuesFromXml(285409313, 285409318, 285409323, 285409328);
                break;
            case 3:
                getValuesFromXml(285409311, 285409316, 285409321, 285409326);
                break;
            case 4:
                getValuesFromXml(285409310, 285409315, 285409320, 285409325);
                this.mAutomaticBrightnessControllerImpl.setAnimationPolicyDisable(true);
                break;
            default:
                return;
        }
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null && (hysteresisLevelsImpl = automaticBrightnessControllerImpl.getHysteresisLevelsImpl()) != null) {
            hysteresisLevelsImpl.createHysteresisThresholdSpline(this.mAmbientBrighteningLux, this.mAmbientBrighteningThresholds, this.mAmbientDarkeningLux, this.mAmbientDarkeningThresholds);
            this.mBrightnessDataProcessor.setExpId(this.mABTestHelper.getExpId());
        }
    }

    private float[] getFloatArray(TypedArray array) {
        int N = array.length();
        float[] vals = new float[N];
        for (int i = 0; i < N; i++) {
            vals[i] = array.getFloat(i, -1.0f);
        }
        array.recycle();
        return vals;
    }
}
