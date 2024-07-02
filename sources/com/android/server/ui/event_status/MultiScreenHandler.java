package com.android.server.ui.event_status;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.util.SparseArray;
import com.android.server.ui.utils.LogUtil;
import java.lang.reflect.Method;
import miui.app.IFreeformCallback;
import miui.app.MiuiFreeFormManager;

/* loaded from: classes.dex */
public class MultiScreenHandler {
    private static final String ACTIVITYTASKMANAGER = "android.app.ActivityTaskManager";
    private static final String METHOD_GETSERVICE = "getService";
    private static final String TAG = "UIService-MultiScreenHandler";
    private static MultiScreenHandler mInstance = null;
    private final Context mContext;
    private boolean mIsSplitScreen = false;
    private boolean mIsSmallScreen = false;
    private boolean mIsMultiScreen = false;
    private final SparseArray<IMultiScreenStatusChangeCallback> mCallbacks = new SparseArray<>();
    private IFreeformCallback mFreeformCallback = new IFreeformCallback.Stub() { // from class: com.android.server.ui.event_status.MultiScreenHandler.1
        public void dispatchFreeFormStackModeChanged(int action, MiuiFreeFormManager.MiuiFreeFormStackInfo stackInfo) {
            switch (action) {
                case 0:
                    MultiScreenHandler.this.mIsSmallScreen = true;
                    LogUtil.logD(MultiScreenHandler.TAG, "FULLSCREEN_TO_FREEFORM:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                case 1:
                    MultiScreenHandler.this.mIsSmallScreen = true;
                    LogUtil.logD(MultiScreenHandler.TAG, "FULLSCREEN_TO_MINIFREEFORM:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                case 2:
                    MultiScreenHandler.this.mIsSmallScreen = true;
                    LogUtil.logD(MultiScreenHandler.TAG, "FREEFORM_TO_MINIFREEFORM:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                case 3:
                    MultiScreenHandler.this.mIsSmallScreen = false;
                    LogUtil.logD(MultiScreenHandler.TAG, "FREEFORM_TO_FULLSCREEN:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                case 4:
                    MultiScreenHandler.this.mIsSmallScreen = true;
                    LogUtil.logD(MultiScreenHandler.TAG, "MINIFREEFORM_TO_FREEFORM:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                case 5:
                    MultiScreenHandler.this.mIsSmallScreen = false;
                    LogUtil.logD(MultiScreenHandler.TAG, "MINIFREEFORM_TO_FULLSCREEN:" + stackInfo.packageName + ", state:" + stackInfo.windowState);
                    break;
                default:
                    LogUtil.logW(MultiScreenHandler.TAG, "warning for access here");
                    break;
            }
            if (!MultiScreenHandler.this.mIsSmallScreen && !MultiScreenHandler.this.mIsSplitScreen) {
                MultiScreenHandler.this.onMultiScreenChanged(false);
            } else {
                MultiScreenHandler.this.onMultiScreenChanged(true);
            }
        }
    };
    private TaskStackListener mTaskStackListener = new TaskStackListener() { // from class: com.android.server.ui.event_status.MultiScreenHandler.2
        public void onTaskStackChanged() {
            MultiScreenHandler multiScreenHandler = MultiScreenHandler.this;
            multiScreenHandler.mIsSplitScreen = multiScreenHandler.isInSplitScreenMode();
            if (MultiScreenHandler.this.mIsSplitScreen || MultiScreenHandler.this.mIsSmallScreen) {
                MultiScreenHandler.this.onMultiScreenChanged(true);
            } else {
                MultiScreenHandler.this.onMultiScreenChanged(false);
            }
            LogUtil.logI(MultiScreenHandler.TAG, "onTaskStackChanged, splitScreenMode: " + MultiScreenHandler.this.mIsSplitScreen + " isSmallScreen: " + MultiScreenHandler.this.mIsSmallScreen);
        }
    };

    /* loaded from: classes.dex */
    public interface IMultiScreenStatusChangeCallback {
        void onChange(boolean z);
    }

    public static synchronized MultiScreenHandler getInstance(Context context) {
        MultiScreenHandler multiScreenHandler;
        synchronized (MultiScreenHandler.class) {
            LogUtil.logD(TAG, "getInstance");
            if (mInstance == null && context != null) {
                mInstance = new MultiScreenHandler(context);
            }
            multiScreenHandler = mInstance;
        }
        return multiScreenHandler;
    }

    private MultiScreenHandler(Context context) {
        this.mContext = context;
        MiuiFreeFormManager.registerFreeformCallback(this.mFreeformCallback);
        registerTaskStackListener();
    }

    public void addMultiScreenStatusChangeCallback(int key, IMultiScreenStatusChangeCallback callback) {
        if (callback == null) {
            return;
        }
        this.mCallbacks.put(key, callback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInSplitScreenMode() {
        try {
            Class<?> clazz = Class.forName(ACTIVITYTASKMANAGER);
            Object obj = clazz.getMethod(METHOD_GETSERVICE, new Class[0]).invoke(null, new Object[0]);
            Method method = obj.getClass().getMethod("isInSplitScreenWindowingMode", new Class[0]);
            return ((Boolean) method.invoke(obj, new Object[0])).booleanValue();
        } catch (Exception e) {
            LogUtil.logE(TAG, "getStackInfo exception : " + e);
            return false;
        }
    }

    private IActivityTaskManager getActivityTaskManager() {
        return ActivityTaskManager.getService();
    }

    public void registerTaskStackListener() {
        try {
            getActivityTaskManager().registerTaskStackListener(this.mTaskStackListener);
        } catch (Exception e) {
            LogUtil.logW(TAG, "Faild to call registerTaskStackListener");
        }
    }

    public void onMultiScreenChanged(boolean isMultiScreen) {
        if (this.mIsMultiScreen != isMultiScreen) {
            LogUtil.logD(TAG, "onMultiScreenChanged, splitScreenMode: " + this.mIsSplitScreen + " isSmallScreen: " + this.mIsSmallScreen);
            this.mIsMultiScreen = isMultiScreen;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                int key = this.mCallbacks.keyAt(i);
                this.mCallbacks.get(key).onChange(this.mIsMultiScreen);
            }
        }
    }
}
