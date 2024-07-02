package com.android.server.wm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.InsetsState;
import android.view.WindowInsets;
import android.window.ClientWindowFrames;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.wm.DisplayArea;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiuiSplitInputMethodImpl implements MiuiSplitInputMethodStub {
    public static final String TAG = "MiuiSplitInputMethodImpl";
    private static HashMap<String, Integer> sSplitMinVersionSupport;
    private Context mContext;
    private Handler mHandler;
    private InputMethodManagerService mImms;
    static boolean DEBUG = false;
    private static boolean sIsSplitIMESupport = false;
    private final Configuration mTmpConfiguration = new Configuration();
    private final Rect mTmpBounds = new Rect();
    private int sVersionCode = -1;
    private boolean mSplitImeSwitch = SystemProperties.getBoolean("persist.spltiIme", true);
    private float mAccuracy = 0.01f;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiSplitInputMethodImpl> {

        /* compiled from: MiuiSplitInputMethodImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiSplitInputMethodImpl INSTANCE = new MiuiSplitInputMethodImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiSplitInputMethodImpl m2684provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiSplitInputMethodImpl m2683provideNewInstance() {
            return new MiuiSplitInputMethodImpl();
        }
    }

    static {
        HashMap<String, Integer> hashMap = new HashMap<>();
        sSplitMinVersionSupport = hashMap;
        hashMap.put("com.iflytek.inputmethod.miui", 8061);
        sSplitMinVersionSupport.put("com.sohu.inputmethod.sogou.xiaomi", 1782);
        sSplitMinVersionSupport.put("com.baidu.input_mi", 1415);
    }

    public void init(InputMethodManagerService imms, Handler handler, Context context) {
        this.mImms = imms;
        this.mHandler = handler;
        this.mContext = context;
        SystemProperties.addChangeCallback(new Runnable() { // from class: com.android.server.wm.MiuiSplitInputMethodImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiSplitInputMethodImpl.this.lambda$init$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$0() {
        this.mSplitImeSwitch = SystemProperties.getBoolean("persist.spltiIme", true);
    }

    public static MiuiSplitInputMethodImpl getInstance() {
        return (MiuiSplitInputMethodImpl) MiuiSplitInputMethodStub.getInstance();
    }

    private boolean isFoldDevice() {
        return "babylon".equals(Build.DEVICE);
    }

    private String getInputMethodPackageName() {
        String curMethod;
        InputMethodManagerService inputMethodManagerService = this.mImms;
        if (inputMethodManagerService == null || (curMethod = (String) invoke(inputMethodManagerService, "getCurIdLocked", null)) == null) {
            return null;
        }
        return curMethod.split("/")[0];
    }

    private int getImeVersionCode() {
        String curMethodPackageName = getInputMethodPackageName();
        try {
            PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(curMethodPackageName, 0);
            if (packageInfo != null) {
                this.sVersionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            this.sVersionCode = -1;
            Slog.e(TAG, "check " + curMethodPackageName + " Version failed", e);
        }
        return this.sVersionCode;
    }

    private boolean isInLargeScreen(Configuration configuration) {
        return configuration != null && configuration.smallestScreenWidthDp >= 600;
    }

    public boolean isSplitIMESupport(Configuration configuration) {
        String curMethodPackageName = getInputMethodPackageName();
        if (this.mContext == null || curMethodPackageName == null) {
            return false;
        }
        sIsSplitIMESupport = true;
        Integer minSupportVersionCode = sSplitMinVersionSupport.get(curMethodPackageName);
        int imeCurVersionCode = getImeVersionCode();
        if (minSupportVersionCode != null && imeCurVersionCode >= minSupportVersionCode.intValue()) {
            sIsSplitIMESupport = true;
        }
        return this.mSplitImeSwitch && sIsSplitIMESupport && isFoldDevice() && isInLargeScreen(configuration);
    }

    public static Object invoke(Object obj, String methodName, Object... values) {
        try {
            Class clazz = obj.getClass();
            if (values == null) {
                Method method = clazz.getDeclaredMethod(methodName, null);
                method.setAccessible(true);
                return method.invoke(obj, new Object[0]);
            }
            Class<?>[] argsClass = new Class[values.length];
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    argsClass[i] = Integer.TYPE;
                } else if (values[i] instanceof Boolean) {
                    argsClass[i] = Boolean.TYPE;
                } else if (values[i] instanceof Float) {
                    argsClass[i] = Float.TYPE;
                } else {
                    argsClass[i] = values[i].getClass();
                }
            }
            Method method2 = clazz.getDeclaredMethod(methodName, argsClass);
            method2.setAccessible(true);
            return method2.invoke(obj, values);
        } catch (Exception e) {
            Slog.d(TAG, "getDeclaredMethod:" + e.toString());
            return null;
        }
    }

    public void adjustWindowFrameForSplitIme(WindowState windowState, ClientWindowFrames clientWindowFrames) {
    }

    private void clearImebounds(Configuration configuration, DisplayArea.Tokens imeWindowsContainer) {
        configuration.seq++;
        this.mTmpBounds.setEmpty();
        invoke(configuration, "updateImeAppBounds", this.mTmpBounds);
        if (imeWindowsContainer != null) {
            imeWindowsContainer.onConfigurationChanged(configuration);
        }
    }

    public void updateImeBoundsInImeTargetChanged(DisplayContent dc, WindowState curFocusedWindow, WindowState newFocusedWindow) {
        if (curFocusedWindow == newFocusedWindow || dc == null) {
            return;
        }
        DisplayArea.Tokens imeWindowsContainer = dc.getImeContainer();
        Configuration configuration = new Configuration(dc.getConfiguration());
        if (!isSplitIMESupport(configuration)) {
            return;
        }
        this.mTmpBounds.setEmpty();
        if (newFocusedWindow == null || !ActivityTaskManagerServiceStub.get().isInSystemSplitScreen(newFocusedWindow)) {
            clearImebounds(configuration, imeWindowsContainer);
            return;
        }
        this.mTmpBounds.set(newFocusedWindow.getBounds());
        float scale = this.mTmpBounds.width() / dc.getBounds().width();
        if (!this.mTmpBounds.equals(invoke(configuration, "getmImeAppBounds", new Object[0])) && Math.abs(scale - 0.5d) < this.mAccuracy) {
            invoke(configuration, "updateImeAppBounds", this.mTmpBounds);
            configuration.seq++;
            if (imeWindowsContainer != null) {
                imeWindowsContainer.onConfigurationChanged(configuration);
            }
        }
    }

    public void adjustCutoutForSplitIme(InsetsState state, WindowState windowState) {
        if (windowState == null || !isSplitIMESupport(windowState.getConfiguration())) {
            return;
        }
        boolean isImeChildWindow = false;
        InsetsControlTarget imeTarget = windowState.mWmService.getDefaultDisplayContentLocked().getImeTarget(0);
        if (imeTarget == null || !ActivityTaskManagerServiceStub.get().isInSystemSplitScreen(imeTarget.getWindow())) {
            return;
        }
        boolean isImeWindow = windowState.getWindowType() == 2011 || windowState.getWindowType() == 2012;
        if (windowState.getParent() != null && (windowState.getParent() instanceof WindowState) && (windowState.getParent().getWindowType() == 2011 || windowState.getParent().getWindowType() == 2012)) {
            isImeChildWindow = true;
        }
        if ((!isImeWindow && !isImeChildWindow) || windowState.getConfiguration().orientation != 2) {
            return;
        }
        state.setDisplayCutout(DisplayCutout.NO_CUTOUT);
        state.removeSource(WindowInsets.Type.displayCutout());
        Slog.d(TAG, "remove cutout for ime");
    }

    public boolean shouldAdjustCutoutForSplitIme(WindowState windowState) {
        InsetsControlTarget imeTarget;
        if (windowState == null || !isSplitIMESupport(windowState.getConfiguration()) || (imeTarget = windowState.mWmService.getDefaultDisplayContentLocked().getImeTarget(0)) == null || !ActivityTaskManagerServiceStub.get().isInSystemSplitScreen(imeTarget.getWindow())) {
            return false;
        }
        boolean isImeWindow = windowState.getWindowType() == 2011 || windowState.getWindowType() == 2012;
        boolean isImeChildWindow = windowState.getParent() != null && (windowState.getParent() instanceof WindowState) && (windowState.getParent().getWindowType() == 2011 || windowState.getParent().getWindowType() == 2012);
        return (isImeWindow || isImeChildWindow) && windowState.getConfiguration().orientation == 2;
    }

    public void onConfigurationChangedForSplitIme(Configuration newParentConfig, DisplayArea.Tokens imeContainer) {
        DisplayContent dc;
        if (newParentConfig == null || imeContainer == null || !this.mSplitImeSwitch || (dc = imeContainer.mDisplayContent) == null) {
            return;
        }
        boolean needToClearImeBounds = false;
        Rect parentImeBounds = (Rect) invoke(newParentConfig, "getmImeAppBounds", new Object[0]);
        Rect imeBounds = (Rect) invoke(imeContainer.getConfiguration(), "getmImeAppBounds", new Object[0]);
        if (parentImeBounds != null && parentImeBounds.isEmpty() && imeBounds != null && !imeBounds.isEmpty()) {
            needToClearImeBounds = true;
        }
        if (!isSplitIMESupport(dc.getConfiguration()) && !needToClearImeBounds) {
            if (DEBUG) {
                Slog.d(TAG, "Ime not support split mode:" + newParentConfig);
            }
        } else if (imeContainer.mWmService != null && imeContainer.mDisplayContent != null) {
            imeContainer.mWmService.mWindowContextListenerController.dispatchPendingConfigurationIfNeeded(imeContainer.mDisplayContent.mDisplayId);
        }
    }

    public void resolveOverrideConfigurationForSplitIme(Configuration newParentConfig, DisplayArea.Tokens imeContainer) {
        if (newParentConfig == null || imeContainer == null || !this.mSplitImeSwitch) {
            return;
        }
        boolean needToClearImeBounds = false;
        Rect parentImeBounds = (Rect) invoke(newParentConfig, "getmImeAppBounds", new Object[0]);
        Rect imeBounds = (Rect) invoke(imeContainer.getConfiguration(), "getmImeAppBounds", new Object[0]);
        if (parentImeBounds != null && parentImeBounds.isEmpty() && imeBounds != null && !imeBounds.isEmpty()) {
            needToClearImeBounds = true;
        }
        if (isSplitIMESupport(newParentConfig) || (!isSplitIMESupport(newParentConfig) && needToClearImeBounds)) {
            imeContainer.getResolvedOverrideConfiguration().setTo(newParentConfig);
        }
    }

    public void updateImeVisiblityIfNeed(boolean visible) {
        InputMethodManagerInternal service = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (service != null && !visible) {
            service.hideCurrentInputMethod(22);
        }
    }

    public void onConfigurationChangedForTask(Configuration newParentConfig, WindowContainer wc) {
        DisplayContent dc;
        DisplayArea.Tokens imeContainer;
        if (!this.mSplitImeSwitch || wc.asTask() == null || (dc = wc.getDisplayContent()) == null || (imeContainer = dc.getImeContainer()) == null) {
            return;
        }
        Configuration preConfiguration = imeContainer.getConfiguration();
        if (!isSplitIMESupport(dc.getConfiguration())) {
            if (invoke(imeContainer.getConfiguration(), "isImeMultiWindowMode", new Object[0]) != null && ((Boolean) invoke(imeContainer.getConfiguration(), "isImeMultiWindowMode", new Object[0])).booleanValue()) {
                clearImebounds(preConfiguration, imeContainer);
            }
            Log.d(TAG, "IME or device not support split ime");
            return;
        }
        if (dc.getImeTarget(0) == null || dc.getImeTarget(0).getWindow() == null) {
            return;
        }
        WindowState imeTarget = dc.getImeTarget(0).getWindow();
        if ((imeTarget.getActivityRecord() == null || imeTarget.getActivityRecord() == wc.asTask().getTopVisibleActivity()) && wc.asTask().affinity != null) {
            float scale = newParentConfig.windowConfiguration.getBounds().width() / dc.getBounds().width();
            boolean isPrevSplitIme = ((Boolean) invoke(imeContainer.getConfiguration(), "isImeMultiWindowMode", new Object[0])).booleanValue();
            boolean isImeTargetSplitMode = Math.abs(((double) scale) - 0.5d) < ((double) this.mAccuracy);
            if (!isImeTargetSplitMode && isPrevSplitIme) {
                clearImebounds(preConfiguration, imeContainer);
                Log.d(TAG, "invalid scale:" + scale + ",clear ime bounds");
                return;
            }
            if (isImeTargetSplitMode) {
                this.mTmpBounds.setEmpty();
                this.mTmpBounds.set(newParentConfig.windowConfiguration.getBounds());
                this.mTmpConfiguration.unset();
                this.mTmpConfiguration.setTo(preConfiguration);
                this.mTmpConfiguration.seq++;
                if (!this.mTmpBounds.equals(invoke(preConfiguration, "getmImeAppBounds", new Object[0]))) {
                    if (!isPrevSplitIme) {
                        updateImeVisiblityIfNeed(false);
                    }
                    invoke(this.mTmpConfiguration, "updateImeAppBounds", this.mTmpBounds);
                    imeContainer.onConfigurationChanged(this.mTmpConfiguration);
                }
            }
        }
    }
}
