package miui.android.animation.property;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import miui.android.animation.utils.KeyUtils;

/* loaded from: classes.dex */
public class ViewPropertyExt {
    public static final BackgroundProperty BACKGROUND;
    public static final ForegroundProperty FOREGROUND;

    private ViewPropertyExt() {
    }

    /* loaded from: classes.dex */
    public static class ForegroundProperty extends ViewProperty implements IIntValueProperty<View> {
        private ForegroundProperty() {
            super("foreground");
        }

        @Override // miui.android.animation.property.FloatProperty
        public float getValue(View object) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }

        @Override // miui.android.animation.property.FloatProperty
        public void setValue(View object, float value) {
        }

        @Override // miui.android.animation.property.IIntValueProperty
        public int getIntValue(View view) {
            Object tag = view.getTag(KeyUtils.KEY_FOLME_FORGROUND_COLOR);
            if (tag instanceof Integer) {
                return ((Integer) tag).intValue();
            }
            return 0;
        }

        @Override // miui.android.animation.property.IIntValueProperty
        public void setIntValue(View view, int value) {
            view.setTag(KeyUtils.KEY_FOLME_FORGROUND_COLOR, Integer.valueOf(value));
            Drawable fg = view.getForeground();
            if (fg != null) {
                fg.invalidateSelf();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class BackgroundProperty extends ViewProperty implements IIntValueProperty<View> {
        private BackgroundProperty() {
            super("background");
        }

        @Override // miui.android.animation.property.FloatProperty
        public float getValue(View object) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }

        @Override // miui.android.animation.property.FloatProperty
        public void setValue(View object, float value) {
        }

        @Override // miui.android.animation.property.IIntValueProperty
        public void setIntValue(View target, int value) {
            target.setBackgroundColor(value);
        }

        @Override // miui.android.animation.property.IIntValueProperty
        public int getIntValue(View target) {
            Drawable bg = target.getBackground();
            if (bg instanceof ColorDrawable) {
                return ((ColorDrawable) bg).getColor();
            }
            return 0;
        }
    }

    static {
        FOREGROUND = new ForegroundProperty();
        BACKGROUND = new BackgroundProperty();
    }
}
