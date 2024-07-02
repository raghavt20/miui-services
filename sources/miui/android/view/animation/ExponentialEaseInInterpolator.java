package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public class ExponentialEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return t == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : (float) Math.pow(2.0d, (t - 1.0f) * 10.0f);
    }
}
