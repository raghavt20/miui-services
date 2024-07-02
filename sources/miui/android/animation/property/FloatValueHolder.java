package miui.android.animation.property;

import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public final class FloatValueHolder {
    private float mValue = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;

    public FloatValueHolder() {
    }

    public FloatValueHolder(float value) {
        setValue(value);
    }

    public void setValue(float value) {
        this.mValue = value;
    }

    public float getValue() {
        return this.mValue;
    }
}
