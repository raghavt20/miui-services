package miui.android.animation.property;

import android.util.Property;
import com.android.server.wm.MiuiMultiWindowRecommendController;

/* loaded from: classes.dex */
public abstract class FloatProperty<T> extends Property<T, Float> {
    final String mPropertyName;

    public abstract float getValue(T t);

    public abstract void setValue(T t, float f);

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.util.Property
    public /* bridge */ /* synthetic */ Float get(Object obj) {
        return get((FloatProperty<T>) obj);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.util.Property
    public /* bridge */ /* synthetic */ void set(Object obj, Float f) {
        set2((FloatProperty<T>) obj, f);
    }

    public FloatProperty(String name) {
        super(Float.class, name);
        this.mPropertyName = name;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // android.util.Property
    public Float get(T object) {
        if (object == null) {
            return Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        }
        return Float.valueOf(getValue(object));
    }

    /* renamed from: set, reason: avoid collision after fix types in other method */
    public final void set2(T object, Float value) {
        if (object != null) {
            setValue(object, value.floatValue());
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "{mPropertyName='" + this.mPropertyName + "'}";
    }
}
