package miui.android.animation.font;

import android.view.View;
import android.widget.TextView;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.lang.ref.WeakReference;
import java.util.Objects;
import miui.android.animation.property.ISpecificProperty;
import miui.android.animation.property.ViewProperty;

/* loaded from: classes.dex */
public class FontWeightProperty extends ViewProperty implements ISpecificProperty {
    private static final String NAME = "fontweight";
    private float mCurWeight;
    private int mFontType;
    private WeakReference<TextView> mTextViewRef;

    public FontWeightProperty(TextView view, int fontType) {
        super(NAME);
        this.mCurWeight = Float.MAX_VALUE;
        this.mTextViewRef = new WeakReference<>(view);
        this.mFontType = fontType;
    }

    public TextView getTextView() {
        return this.mTextViewRef.get();
    }

    public float getScaledTextSize() {
        TextView textView = this.mTextViewRef.get();
        if (textView != null) {
            return textView.getTextSize() / textView.getResources().getDisplayMetrics().scaledDensity;
        }
        return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    @Override // miui.android.animation.property.FloatProperty
    public float getValue(View object) {
        return this.mCurWeight;
    }

    @Override // miui.android.animation.property.FloatProperty
    public void setValue(View object, float value) {
        this.mCurWeight = value;
        TextView textView = this.mTextViewRef.get();
        if (textView != null) {
            VarFontUtils.setVariationFont(textView, (int) value);
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass() || !super.equals(object)) {
            return false;
        }
        FontWeightProperty that = (FontWeightProperty) object;
        TextView textView = this.mTextViewRef.get();
        TextView textView1 = that.mTextViewRef.get();
        if (textView != null && textView.equals(textView1)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        TextView textView = this.mTextViewRef.get();
        if (textView != null) {
            return Objects.hash(Integer.valueOf(super.hashCode()), textView);
        }
        return Objects.hash(Integer.valueOf(super.hashCode()), this.mTextViewRef);
    }

    @Override // miui.android.animation.property.ISpecificProperty
    public float getSpecificValue(float value) {
        TextView textView = this.mTextViewRef.get();
        if (value < VarFontUtils.MIN_WGHT && textView != null) {
            int sysScale = VarFontUtils.getSysFontScale(textView.getContext());
            return VarFontUtils.getScaleWght((int) value, getScaledTextSize(), this.mFontType, sysScale);
        }
        return value;
    }
}
