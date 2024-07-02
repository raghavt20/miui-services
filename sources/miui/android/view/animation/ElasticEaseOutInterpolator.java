package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public class ElasticEaseOutInterpolator implements Interpolator {
    private final float mAmplitude;
    private final float mPeriod;

    public ElasticEaseOutInterpolator() {
        this(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
    }

    public ElasticEaseOutInterpolator(float amplitude, float period) {
        this.mAmplitude = amplitude;
        this.mPeriod = period;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float s;
        float p = this.mPeriod;
        float a = this.mAmplitude;
        if (t == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        if (t == 1.0f) {
            return 1.0f;
        }
        if (p == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            p = 0.3f;
        }
        if (a != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && a >= 1.0f) {
            s = (float) ((p / 6.283185307179586d) * Math.asin(1.0f / a));
        } else {
            a = 1.0f;
            s = p / 4.0f;
        }
        return (float) ((a * Math.pow(2.0d, (-10.0f) * t) * Math.sin(((t - s) * 6.283185307179586d) / p)) + 1.0d);
    }
}
