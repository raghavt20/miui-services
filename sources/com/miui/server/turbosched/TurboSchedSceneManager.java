package com.miui.server.turbosched;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.TurboSchedMonitor;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.lang.reflect.Method;
import java.util.Map;
import miui.app.IFreeformCallback;
import miui.app.MiuiFreeFormManager;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;
import miui.turbosched.ITurboSchedManager;

/* loaded from: classes.dex */
public class TurboSchedSceneManager {
    private static final String ACTIVITYTASKMANAGER = "android.app.ActivityTaskManager";
    private static final int CURR_FOCUSED = 1;
    private static final int FULL_SCREEN = -1;
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int LAST_FOCUSED = 0;
    private static final String METHOD_GETSERVICE = "getService";
    private static final int MINI_SCREEN = 1;
    private static final int MSG_FREEFORM_MODE_CHANGED = 11;
    private static final int MSG_ON_FOCUSED_APP_CHANGED = 3;
    private static final int MSG_ON_FOREGROUND_APP_CHANGED = 2;
    private static final int MSG_ON_TASK_STACK_CHANGE = 1;
    private static final String PERMISSION_PKG_NAME = "com.lbe.security.miui";
    private static final int SMALL_SCREEN = 0;
    private static final String TAG = "tsched_scene";
    private IActivityTaskManager mActivityTaskManager;
    private ArrayMap<String, ITurboSchedManager.ITurboSchedStateChangeCallback> mAppStateListeners;
    private Context mContext;
    private String mCurFocusedPkgName;
    private String mForegroundAppName;
    private IFreeformCallback mFreeformCallback;
    private boolean mIsGameMode;
    private boolean mIsMiniScreen;
    private boolean mIsSmallScreen;
    private boolean mIsSplitScreen;
    private String mLastFocusedPkgName;
    private final BroadcastReceiver mLifecycleReceiver;
    private final Object mLock;
    private final TaskStackListener mTaskStackListener;
    private TurboSchedSceneHandler mTbSceneHandler;
    private final IForegroundWindowListener mWindowListener;

    public static TurboSchedSceneManager getInstance() {
        return TurboSchedSceneManagerHolder.sInstance;
    }

    private TurboSchedSceneManager() {
        this.mIsSmallScreen = false;
        this.mIsMiniScreen = false;
        this.mIsSplitScreen = false;
        this.mIsGameMode = false;
        this.mLastFocusedPkgName = "";
        this.mCurFocusedPkgName = "";
        this.mLock = new Object();
        this.mAppStateListeners = null;
        this.mLifecycleReceiver = new BroadcastReceiver() { // from class: com.miui.server.turbosched.TurboSchedSceneManager.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                if (intent.getAction() == null) {
                    return;
                }
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case 798292259:
                        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        if (TurboSchedMonitor.getInstance().isDebugMode()) {
                            Slog.i(TurboSchedSceneManager.TAG, "turbosched scene manager receive boot complete message");
                        }
                        TurboSchedSceneManager.this.onBootComplete();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mTaskStackListener = new TaskStackListener() { // from class: com.miui.server.turbosched.TurboSchedSceneManager.3
            public void onTaskStackChanged() {
                TurboSchedSceneManager.this.mTbSceneHandler.removeMessages(1);
                Message msg = TurboSchedSceneManager.this.mTbSceneHandler.obtainMessage(1);
                TurboSchedSceneManager.this.mTbSceneHandler.sendMessage(msg);
            }
        };
        this.mWindowListener = new IForegroundWindowListener.Stub() { // from class: com.miui.server.turbosched.TurboSchedSceneManager.4
            public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
                Message msg = TurboSchedSceneManager.this.mTbSceneHandler.obtainMessage(2, foregroundInfo);
                TurboSchedSceneManager.this.mTbSceneHandler.sendMessage(msg);
            }
        };
        this.mFreeformCallback = new IFreeformCallback.Stub() { // from class: com.miui.server.turbosched.TurboSchedSceneManager.5
            public void dispatchFreeFormStackModeChanged(int action, MiuiFreeFormManager.MiuiFreeFormStackInfo stackInfo) {
                Bundle bundle = new Bundle();
                bundle.putInt("action", action);
                bundle.putString("pkgName", stackInfo.packageName);
                bundle.putInt("windowState", stackInfo.windowState);
                Message msg = TurboSchedSceneManager.this.mTbSceneHandler.obtainMessage();
                msg.setData(bundle);
                msg.what = 11;
                TurboSchedSceneManager.this.mTbSceneHandler.sendMessage(msg);
            }
        };
        this.mAppStateListeners = new ArrayMap<>();
        this.mActivityTaskManager = ActivityTaskManager.getService();
    }

    public void initialize(Context context, Looper looper) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mLifecycleReceiver, intentFilter);
        this.mTbSceneHandler = new TurboSchedSceneHandler(looper);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBootComplete() {
        registerCommonListener();
    }

    private void registerCommonListener() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            ProcessManager.registerForegroundWindowListener(this.mWindowListener);
            MiuiFreeFormManager.registerFreeformCallback(this.mFreeformCallback);
        } catch (RemoteException e) {
            Slog.e(TAG, "register common listener failed", e);
        }
    }

    public void registerStateChangeCallback(final String pkgName, final ITurboSchedManager.ITurboSchedStateChangeCallback cb) {
        IBinder.DeathRecipient dr = new IBinder.DeathRecipient() { // from class: com.miui.server.turbosched.TurboSchedSceneManager.1
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                Slog.i(TurboSchedSceneManager.TAG, "binder died pkg=" + pkgName + ", cb=" + cb);
                cb.asBinder().unlinkToDeath(this, 0);
                TurboSchedSceneManager.this.unregisterStateChangeCallback(pkgName, cb);
            }
        };
        try {
            cb.asBinder().linkToDeath(dr, 0);
            synchronized (this.mLock) {
                this.mAppStateListeners.put(pkgName, cb);
            }
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.i(TAG, "register window state callback pkg=" + pkgName + " listener size=" + this.mAppStateListeners.size() + ", " + this.mAppStateListeners.get(pkgName));
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error on linkToDeath - ", e);
        }
    }

    public void unregisterStateChangeCallback(String pkgName, ITurboSchedManager.ITurboSchedStateChangeCallback cb) {
        synchronized (this.mLock) {
            if (this.mAppStateListeners.containsKey(pkgName)) {
                this.mAppStateListeners.remove(pkgName);
            }
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.i(TAG, "unregister window state callback pkg=" + pkgName + " listener size=" + this.mAppStateListeners.size());
        }
    }

    private void setPkgWindowState(String pkgName, int windowState) {
        try {
            synchronized (this.mLock) {
                ITurboSchedManager.ITurboSchedStateChangeCallback listener = this.mAppStateListeners.get(pkgName);
                if (listener != null) {
                    listener.onScreenStateChanged(pkgName, windowState);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "setPkgWindowState catch remoteException", e);
        }
    }

    private void setFocusedPkgState(String pkgName, boolean isFocused) {
        try {
            synchronized (this.mLock) {
                ITurboSchedManager.ITurboSchedStateChangeCallback listener = this.mAppStateListeners.get(pkgName);
                if (listener != null) {
                    listener.onFocusedWindowChange(pkgName, isFocused);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "setFocusedPkgState catch remoteException", e);
        }
    }

    private void notifyInGameMode(boolean state) {
        try {
            synchronized (this.mLock) {
                for (Map.Entry<String, ITurboSchedManager.ITurboSchedStateChangeCallback> entry : this.mAppStateListeners.entrySet()) {
                    entry.getKey();
                    ITurboSchedManager.ITurboSchedStateChangeCallback listener = entry.getValue();
                    if (listener != null) {
                        listener.onGameModeChange(state);
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "setFocusedPkgState catch remoteException", e);
        }
    }

    private boolean isInSplitScreenMode() {
        try {
            Class<?> clazz = Class.forName(ACTIVITYTASKMANAGER);
            Object obj = clazz.getMethod(METHOD_GETSERVICE, new Class[0]).invoke(null, new Object[0]);
            Method method = obj.getClass().getMethod("isInSplitScreenWindowingMode", new Class[0]);
            return ((Boolean) method.invoke(obj, new Object[0])).booleanValue();
        } catch (Exception e) {
            Slog.e(TAG, "getStackInfo exception : " + e);
            return false;
        }
    }

    private boolean isGameMode() {
        boolean ret = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gb_boosting", 0, -2) == 1;
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void makeTurboSchedDesion() {
        boolean gameMode = isGameMode();
        if (gameMode != this.mIsGameMode) {
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.i(TAG, "make turbo sched desion, GameMode: " + this.mIsGameMode);
            }
            this.mIsGameMode = gameMode;
            notifyInGameMode(gameMode);
        }
    }

    public void onFocusedWindowChangeLocked(String focus, int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, type);
        bundle.putString("focus", focus);
        Message msg = this.mTbSceneHandler.obtainMessage();
        msg.setData(bundle);
        msg.what = 3;
        this.mTbSceneHandler.sendMessage(msg);
    }

    private void handleTaskStackChange() {
        this.mIsSplitScreen = isInSplitScreenMode();
        this.mIsGameMode = isGameMode();
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.i(TAG, "onTaskStackChanged, splitScreenMode: " + this.mIsSplitScreen + " SmallScreen: " + this.mIsSmallScreen + " GameMode:" + this.mIsGameMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWindowChanged(ForegroundInfo foregroundInfo) {
        String packageName = foregroundInfo.mForegroundPackageName;
        if (!packageName.equals(PERMISSION_PKG_NAME) && !TextUtils.equals(this.mForegroundAppName, packageName)) {
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.d(TAG, "windowChanged: fg pkg from " + this.mForegroundAppName + " to " + packageName);
            }
            this.mForegroundAppName = packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFocusAppChange(Message msg) {
        Bundle data = msg.getData();
        int type = data.getInt(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE);
        String focus = data.getString("focus");
        switch (type) {
            case 0:
                this.mLastFocusedPkgName = focus;
                setFocusedPkgState(focus, false);
                return;
            case 1:
                this.mCurFocusedPkgName = focus;
                setFocusedPkgState(focus, true);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFreeformModeChanged(Message msg) {
        Bundle data = msg.getData();
        int action = data.getInt("action");
        String pkgName = data.getString("pkgName");
        int windowState = data.getInt("windowState");
        switch (action) {
            case 0:
                this.mIsSmallScreen = true;
                this.mIsMiniScreen = false;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "FULLSCREEN_TO_FREEFORM:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 1:
                this.mIsSmallScreen = true;
                this.mIsMiniScreen = true;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "FULLSCREEN_TO_MINIFREEFORM:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 2:
                this.mIsSmallScreen = true;
                this.mIsMiniScreen = true;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "FREEFORM_TO_MINIFREEFORM:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 3:
                this.mIsSmallScreen = false;
                this.mIsMiniScreen = false;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "FREEFORM_TO_FULLSCREEN:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 4:
                this.mIsSmallScreen = true;
                this.mIsMiniScreen = false;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "MINIFREEFORM_TO_FREEFORM:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 5:
                this.mIsSmallScreen = false;
                this.mIsMiniScreen = false;
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "MINIFREEFORM_TO_FULLSCREEN:" + pkgName + ", state:" + windowState);
                    break;
                }
                break;
            case 6:
            case 7:
            case 8:
            default:
                Slog.w(TAG, "warning for access here action=" + action + ", windowstate=" + windowState);
                return;
            case 9:
            case 10:
            case 11:
            case 12:
                break;
        }
        setPkgWindowState(pkgName, windowState);
    }

    /* loaded from: classes.dex */
    class TurboSchedSceneHandler extends Handler {
        public TurboSchedSceneHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 2:
                    TurboSchedSceneManager.this.handleWindowChanged((ForegroundInfo) msg.obj);
                    break;
                case 3:
                    TurboSchedSceneManager.this.handleFocusAppChange(msg);
                    break;
                case 11:
                    TurboSchedSceneManager.this.handleFreeformModeChanged(msg);
                    break;
            }
            TurboSchedSceneManager.this.makeTurboSchedDesion();
        }
    }

    /* loaded from: classes.dex */
    class TurboSchedSceneManagerHolder {
        static final TurboSchedSceneManager sInstance = new TurboSchedSceneManager();

        TurboSchedSceneManagerHolder() {
        }
    }
}
