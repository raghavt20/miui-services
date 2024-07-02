package com.android.server.wm;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.RectF;
import android.os.SystemProperties;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.SurfaceControl;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class RecommendUtils {
    public static final boolean MIUI_FREEFORM_SHADOW_V2_SUPPORTED = SystemProperties.getBoolean("persist.sys.mi_shadow_supported", true);
    static final String TAG = "RecommendUtils";

    private RecommendUtils() {
    }

    public static boolean isMiuiBuild() {
        return SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    public static boolean isInSplitScreenWindowingMode(Task task) {
        if (task == null) {
            return false;
        }
        try {
            boolean isRoot = task.isRootTask();
            if (isRoot) {
                return task.mCreatedByOrganizer && task.hasChild() && task.getWindowingMode() == 1 && task.getTopChild() != null && task.getTopChild().asTask() != null && task.getTopChild().getWindowingMode() == 6;
            }
            Task rootTask = task.getRootTask();
            return rootTask != null && rootTask.mCreatedByOrganizer && rootTask.hasChild() && rootTask.getWindowingMode() == 1 && rootTask.getTopChild() != null && rootTask.getTopChild().asTask() != null && rootTask.getTopChild().getWindowingMode() == 6;
        } catch (Exception e) {
            Slog.d(TAG, "exception in isInSplitScreenWindowingMode:" + e.getMessage());
            return false;
        }
    }

    public static boolean isInEmbeddedWindowingMode(Task task) {
        if (task == null || task == null) {
            return false;
        }
        try {
            if (task.hasChild() && task.getWindowingMode() == 1) {
                return task.getTopChild().isEmbedded();
            }
            return false;
        } catch (Exception e) {
            Slog.d(TAG, "exception in isInSplitScreenWindowingMode:" + e.getMessage());
            return false;
        }
    }

    public static boolean isSupportMiuiMultiWindowRecommend() {
        return true;
    }

    public static void setCornerRadiusForSurfaceControl(SurfaceControl surface, int radius) {
        if (surface != null && surface.isValid()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setCornerRadius(surface, radius);
            t.apply();
        }
    }

    public static void setShadowSettingsInTransactionForSurfaceControl(SurfaceControl surface, int shadowType, float length, float[] color, float offsetX, float offsetY, float outset, int numOfLayers) {
        if (surface != null && surface.isValid() && MiuiMultiWindowUtils.isSupportMiuiShadow()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
            Object[] values = {surface, Integer.valueOf(shadowType), Float.valueOf(length), color, Float.valueOf(offsetX), Float.valueOf(offsetY), Float.valueOf(outset), Integer.valueOf(numOfLayers)};
            MiuiMultiWindowUtils.callObjectMethod(t, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes, values);
            t.apply();
        }
    }

    public static void resetShadowSettingsInTransactionForSurfaceControl(SurfaceControl surface, int shadowType, float length, float[] color, float offsetX, float offsetY, float outset, int numOfLayers) {
        setShadowSettingsInTransactionForSurfaceControl(surface, shadowType, length, color, offsetX, offsetY, outset, numOfLayers);
    }

    public static boolean isKeyguardLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
    }

    public static boolean isSupportMiuiShadowV2() {
        Method setShadowSettingsMethod;
        if (!MIUI_FREEFORM_SHADOW_V2_SUPPORTED) {
            return false;
        }
        Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE};
        try {
            setShadowSettingsMethod = MiuiMultiWindowUtils.getDeclaredMethod(SurfaceControl.Transaction.class, "setMiShadow", parameterTypes);
        } catch (NoSuchMethodException e) {
        }
        return setShadowSettingsMethod != null;
    }

    public static void setMiShadowV2(SurfaceControl sc, int color, float offsetX, float offsetY, float radius, float dispersion, float cornerRadius, RectF bounds, Context context) {
        if (sc != null && sc.isValid()) {
            if (context != null) {
                SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Boolean.TYPE, RectF.class};
                Object[] values = {sc, Integer.valueOf(color), Float.valueOf(applyDip2Px(context, offsetX)), Float.valueOf(applyDip2Px(context, offsetY)), Float.valueOf(applyDip2Px(context, radius)), Float.valueOf(dispersion), Float.valueOf(applyDip2Px(context, cornerRadius)), false, bounds};
                MiuiMultiWindowUtils.callObjectMethod(t, SurfaceControl.Transaction.class, "setMiShadow", parameterTypes, values);
                t.apply();
                return;
            }
        }
        Slog.e(TAG, "invalid SurfaceControl = " + sc);
    }

    public static void resetMiShadowV2(SurfaceControl sc, Context context) {
        if (sc == null || !sc.isValid() || context == null) {
            Slog.e(TAG, "invalid SurfaceControl = " + sc);
        } else {
            setMiShadowV2(sc, 0, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, new RectF(), context);
        }
    }

    public static float applyDip2Px(Context context, float value) {
        return TypedValue.applyDimension(1, value, context.getResources().getDisplayMetrics());
    }
}
