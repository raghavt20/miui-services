package miui.android.animation.property;

import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.util.Objects;

/* loaded from: classes.dex */
public class ColorProperty<T> extends FloatProperty<T> implements IIntValueProperty<T> {
    private int mColorValue;

    public ColorProperty(String name) {
        super(name);
    }

    @Override // miui.android.animation.property.FloatProperty
    public void setValue(T object, float value) {
    }

    @Override // miui.android.animation.property.FloatProperty
    public float getValue(T o) {
        return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // miui.android.animation.property.IIntValueProperty
    public void setIntValue(T t, int i) {
        this.mColorValue = i;
        if (t instanceof ValueTargetObject) {
            ValueTargetObject obj = (ValueTargetObject) t;
            obj.setPropertyValue(getName(), Integer.TYPE, Integer.valueOf(i));
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // miui.android.animation.property.IIntValueProperty
    public int getIntValue(T t) {
        if (t instanceof ValueTargetObject) {
            ValueTargetObject obj = (ValueTargetObject) t;
            this.mColorValue = ((Integer) obj.getPropertyValue(getName(), Integer.TYPE)).intValue();
        }
        return this.mColorValue;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColorProperty that = (ColorProperty) o;
        return this.mPropertyName.equals(that.mPropertyName);
    }

    public int hashCode() {
        return Objects.hash(this.mPropertyName);
    }
}
