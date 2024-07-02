package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public class ExponentialEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        if (t == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        if (t == 1.0f) {
            return 1.0f;
        }
        if (t * 2.0f < 1.0f) {
            return ((float) Math.pow(2.0d, (r6 - 1.0f) * 10.0f)) * 0.5f;
        }
        return ((float) ((-Math.pow(2.0d, (-10.0f) * (r6 - 1.0f))) + 2.0d)) * 0.5f;
    }
}
