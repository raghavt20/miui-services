package com.android.server.camera;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes.dex */
public class CameraOpt implements CameraOptStub {
    private static boolean IS_SUPPORT_ABIS = false;
    private static final String TAG = "CameraOpt";
    private static Class<?> mCameraOptManager;
    private static MethodInfo sMethodInfo = new MethodInfo();
    private static volatile Map<MethodInfo, Method> mMethodInfoMap = new HashMap();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<CameraOpt> {

        /* compiled from: CameraOpt$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final CameraOpt INSTANCE = new CameraOpt();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public CameraOpt m905provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public CameraOpt m904provideNewInstance() {
            return new CameraOpt();
        }
    }

    static {
        try {
            IS_SUPPORT_ABIS = Build.SUPPORTED_ABIS[0] != null ? Build.SUPPORTED_ABIS[0].startsWith("arm") : false;
            PathClassLoader pathClassLoader = (PathClassLoader) CameraOpt.class.getClassLoader();
            pathClassLoader.addDexPath("/system_ext/framework/miui-cameraopt.jar");
            mCameraOptManager = Class.forName("com.miui.cameraopt.CameraOptManager");
        } catch (Exception e) {
            Slog.e(TAG, "ClassNotFound from /system_ext/framework/miui-cameraopt.jar, " + e);
        }
    }

    private static Method getDeclaredMethod(Class clazz, String methodName, Class<?>[] paramsTypes) {
        Method declaredMethod = null;
        if (paramsTypes != null) {
            try {
            } catch (Exception e) {
                Slog.e(TAG, "getDeclaredMethod:" + methodName + " failed");
            }
            if (paramsTypes.length > 0) {
                declaredMethod = clazz.getDeclaredMethod(methodName, paramsTypes);
                return declaredMethod;
            }
        }
        declaredMethod = clazz.getDeclaredMethod(methodName, new Class[0]);
        return declaredMethod;
    }

    private static Method getFuncMethod(Class clazz, String methodName, Class[] paramsTypes) {
        synchronized (sMethodInfo) {
            sMethodInfo.setClass(clazz).setMethodName(methodName).setParmasClass(paramsTypes);
        }
        try {
            if (mMethodInfoMap.containsKey(sMethodInfo)) {
                return mMethodInfoMap.get(sMethodInfo);
            }
            Method method = getDeclaredMethod(clazz, methodName, paramsTypes);
            if (method == null) {
                return null;
            }
            synchronized (mMethodInfoMap) {
                mMethodInfoMap.put(new MethodInfo(clazz, methodName, paramsTypes), method);
            }
            return method;
        } catch (Exception e) {
            Slog.w(TAG, "getFuncMethod Fail!");
            return null;
        }
    }

    public static Object callMethod(String funcName, Object... values) {
        if (!IS_SUPPORT_ABIS) {
            return null;
        }
        Class<?> cls = mCameraOptManager;
        if (cls == null) {
            Slog.w(TAG, "call methods(" + funcName + ") failed, because cameraopt was null.");
            return null;
        }
        return callStaticMethod(cls, funcName, values);
    }

    public static Object callStaticMethod(Class clazz, String funcName, Object... values) {
        if (clazz == null) {
            return null;
        }
        try {
            Method declaredMethod = getFuncMethod(clazz, funcName, getParameterTypes(values));
            declaredMethod.setAccessible(true);
            if (values != null && values.length > 0) {
                return declaredMethod.invoke(null, values);
            }
            return declaredMethod.invoke(null, new Object[0]);
        } catch (Exception e) {
            Slog.w(TAG, "call methods(" + funcName + ") failed :" + e);
            return null;
        }
    }

    private static Class[] getParameterTypes(Object... args) {
        if (args == null) {
            return null;
        }
        Class[] parameterTypes = new Class[args.length];
        int j = args.length;
        for (int i = 0; i < j; i++) {
            if (args[i] == null) {
                Slog.w(TAG, "the params of method is null, so return. index : " + i);
                return null;
            }
            if (args[i] instanceof Integer) {
                parameterTypes[i] = Integer.TYPE;
            } else if (args[i] instanceof Double) {
                parameterTypes[i] = Double.TYPE;
            } else if (args[i] instanceof Long) {
                parameterTypes[i] = Long.TYPE;
            } else if (args[i] instanceof Boolean) {
                parameterTypes[i] = Boolean.TYPE;
            } else if (args[i] instanceof Context) {
                parameterTypes[i] = Context.class;
            } else {
                parameterTypes[i] = args[i].getClass();
            }
        }
        return parameterTypes;
    }

    public void onTransitionAnimateStateChanged(boolean isAnimationStart) {
        callMethod("onTransitionAnimateStateChanged", Boolean.valueOf(isAnimationStart));
    }

    public boolean interceptAppRestartIfNeeded(String processName, String type) {
        Object result = callMethod("interceptAppRestartIfNeeded", processName, type);
        return result != null && ((Boolean) result).booleanValue();
    }

    public void boostCameraByThreshold(Intent in) {
        if (isNeedBoostCamera(in)) {
            callMethod("boostCameraByThreshold", 0L);
        }
    }

    private boolean isNeedBoostCamera(Intent in) {
        if (in == null) {
            return false;
        }
        ComponentName comp = in.getComponent();
        String action = in.getAction();
        if (comp == null || action == null) {
            return false;
        }
        if (!TextUtils.equals(comp.flattenToShortString(), "com.android.camera/.Camera") && !TextUtils.equals(comp.flattenToShortString(), "com.android.camera/.VoiceCamera")) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MethodInfo {
        Class mClazz;
        String mMethodName;
        Class[] mParamsTypes;

        MethodInfo() {
        }

        MethodInfo(Class clazz, String methodName, Class[] paramsTypes) {
            this.mClazz = clazz;
            this.mMethodName = methodName;
            this.mParamsTypes = paramsTypes;
        }

        public int hashCode() {
            Class cls = this.mClazz;
            int result = cls != null ? cls.hashCode() : 0;
            int i = result * 31;
            String str = this.mMethodName;
            int result2 = i + (str != null ? str.hashCode() : 0) + Arrays.hashCode(this.mParamsTypes);
            return result2;
        }

        public boolean equals(Object o) {
            if (Objects.isNull(o)) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (getClass() != o.getClass()) {
                return false;
            }
            MethodInfo methodInfo = (MethodInfo) o;
            return this.mClazz == methodInfo.mClazz && this.mMethodName == methodInfo.mMethodName && Arrays.equals(this.mParamsTypes, methodInfo.mParamsTypes);
        }

        public MethodInfo setClass(Class clazz) {
            this.mClazz = clazz;
            return this;
        }

        public MethodInfo setMethodName(String methodName) {
            this.mMethodName = methodName;
            return this;
        }

        public MethodInfo setParmasClass(Class[] paramsTypes) {
            this.mParamsTypes = paramsTypes;
            return this;
        }
    }
}
