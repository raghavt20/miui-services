package miui.android.animation.utils;

import android.animation.ArgbEvaluator;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import miui.android.animation.IAnimTarget;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.ViewProperty;

/* loaded from: classes.dex */
public class CommonUtils {
    public static final String TAG = "miuix_anim";
    public static final int UNIT_SECOND = 1000;
    private static float sTouchSlop;
    public static final ArgbEvaluator sArgbEvaluator = new ArgbEvaluator();
    private static final Class<?>[] BUILT_IN = {String.class, Integer.TYPE, Integer.class, Long.TYPE, Long.class, Short.TYPE, Short.class, Float.TYPE, Float.class, Double.TYPE, Double.class};

    private CommonUtils() {
    }

    /* loaded from: classes.dex */
    private static class OnPreDrawTask implements ViewTreeObserver.OnPreDrawListener {
        Runnable mTask;
        WeakReference<View> mView;

        OnPreDrawTask(Runnable task) {
            this.mTask = task;
        }

        public void start(View view) {
            ViewTreeObserver observer = view.getViewTreeObserver();
            this.mView = new WeakReference<>(view);
            observer.addOnPreDrawListener(this);
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {
            View view = this.mView.get();
            if (view != null) {
                Runnable runnable = this.mTask;
                if (runnable != null) {
                    runnable.run();
                }
                view.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            this.mTask = null;
            return true;
        }
    }

    public static void runOnPreDraw(View view, Runnable task) {
        if (view == null) {
            return;
        }
        new OnPreDrawTask(task).start(view);
    }

    public static int[] toIntArray(float[] floatArray) {
        int[] intArray = new int[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            intArray[i] = (int) floatArray[i];
        }
        return intArray;
    }

    public static String mapsToString(Map[] maps) {
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; i < maps.length; i++) {
            b.append('\n');
            b.append(i).append('.').append((CharSequence) mapToString(maps[i], "    "));
        }
        b.append('\n').append(']');
        return b.toString();
    }

    public static <K, V> StringBuilder mapToString(Map<K, V> map, String prefix) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        if (map != null && map.size() > 0) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                b.append('\n').append(prefix);
                b.append(entry.getKey()).append('=').append(entry.getValue());
            }
            b.append('\n');
        }
        b.append('}');
        return b;
    }

    @SafeVarargs
    public static <T> T[] mergeArray(T[] tArr, T... tArr2) {
        if (tArr == null) {
            return tArr2;
        }
        if (tArr2 == null) {
            return tArr;
        }
        Object newInstance = Array.newInstance(tArr.getClass().getComponentType(), tArr.length + tArr2.length);
        System.arraycopy(tArr, 0, newInstance, 0, tArr.length);
        System.arraycopy(tArr2, 0, newInstance, tArr.length, tArr2.length);
        return (T[]) ((Object[]) newInstance);
    }

    public static boolean hasFlags(long flags, long mask) {
        return (flags & mask) != 0;
    }

    public static int toIntValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        if (value instanceof Float) {
            return ((Float) value).intValue();
        }
        throw new IllegalArgumentException("toFloat failed, value is " + value);
    }

    public static float toFloatValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).floatValue();
        }
        if (value instanceof Float) {
            return ((Float) value).floatValue();
        }
        throw new IllegalArgumentException("toFloat failed, value is " + value);
    }

    public static <T> boolean isArrayEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean inArray(T[] array, T element) {
        if (element != null && array != null && array.length > 0) {
            for (T item : array) {
                if (item.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float getTouchSlop(View target) {
        if (sTouchSlop == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && target != null) {
            sTouchSlop = ViewConfiguration.get(target.getContext()).getScaledTouchSlop();
        }
        return sTouchSlop;
    }

    public static double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2.0d) + Math.pow(y2 - y1, 2.0d));
    }

    public static void getRect(IAnimTarget target, RectF rect) {
        rect.left = target.getValue(ViewProperty.X);
        rect.top = target.getValue(ViewProperty.Y);
        rect.right = rect.left + target.getValue(ViewProperty.WIDTH);
        rect.bottom = rect.top + target.getValue(ViewProperty.HEIGHT);
    }

    public static float getSize(IAnimTarget target, FloatProperty property) {
        FloatProperty sizeProp = null;
        if (property == ViewProperty.X) {
            sizeProp = ViewProperty.WIDTH;
        } else if (property == ViewProperty.Y) {
            sizeProp = ViewProperty.HEIGHT;
        } else if (property == ViewProperty.WIDTH || property == ViewProperty.HEIGHT) {
            sizeProp = property;
        }
        return sizeProp == null ? MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X : target.getValue(sizeProp);
    }

    public static boolean isBuiltInClass(Class<?> clz) {
        return inArray(BUILT_IN, clz);
    }

    public static <T> void addTo(Collection<T> src, Collection<T> dst) {
        for (T t : src) {
            if (!dst.contains(t)) {
                dst.add(t);
            }
        }
    }

    public static String readProp(String prop) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            try {
                Process process = Runtime.getRuntime().exec("getprop " + prop);
                ir = new InputStreamReader(process.getInputStream());
                input = new BufferedReader(ir);
                return input.readLine();
            } catch (IOException e) {
                Log.i(TAG, "readProp failed", e);
                closeQuietly(input);
                closeQuietly(ir);
                return "";
            }
        } finally {
            closeQuietly(input);
            closeQuietly(ir);
        }
    }

    private static void closeQuietly(Closeable io) {
        if (io != null) {
            try {
                io.close();
            } catch (Exception e) {
                Log.w(TAG, "close " + io + " failed", e);
            }
        }
    }

    public static <T> T getLocal(ThreadLocal<T> threadLocal, Class cls) {
        T t = threadLocal.get();
        if (t == null && cls != null) {
            T t2 = (T) ObjectPool.acquire(cls, new Object[0]);
            threadLocal.set(t2);
            return t2;
        }
        return t;
    }
}
