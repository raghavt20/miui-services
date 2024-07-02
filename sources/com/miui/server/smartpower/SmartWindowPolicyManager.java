package com.miui.server.smartpower;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.am.SmartPowerService;
import com.android.server.wm.WindowManagerService;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.app.smartpower.SmartPowerSettings;
import java.io.PrintWriter;
import miui.smartpower.MultiTaskActionManager;

/* loaded from: classes.dex */
public class SmartWindowPolicyManager {
    private static final int EVENT_MULTI_TASK_ACTION_END = 2;
    private static final int EVENT_MULTI_TASK_ACTION_START = 1;
    private static final int MSG_MULTI_TASK_ACTION_CHANGED = 1;
    public static final String TAG = "SmartPower.WindowPolicy";
    private final Context mContext;
    private final H mHandler;
    private MultiTaskPolicyController mMultiTaskPolicyController;
    private final WindowManagerService mWMS;
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static boolean sEnable = SmartPowerSettings.WINDOW_POLICY_ENABLE;
    private SmartDisplayPolicyManager mSmartDisplayPolicyManager = null;
    private SmartBoostPolicyManager mSmartBoostPolicyManager = null;

    public SmartWindowPolicyManager(Context context, Looper looper, WindowManagerService wms) {
        this.mMultiTaskPolicyController = null;
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mWMS = wms;
        this.mMultiTaskPolicyController = new MultiTaskPolicyController();
    }

    public void init(SmartDisplayPolicyManager smartDisplayPolicyManager, SmartBoostPolicyManager smartBoostPolicyManager) {
        this.mSmartDisplayPolicyManager = smartDisplayPolicyManager;
        this.mSmartBoostPolicyManager = smartBoostPolicyManager;
    }

    public boolean registerMultiTaskActionListener(int listenerFlag, Handler handler, SmartPowerServiceInternal.MultiTaskActionListener listener) {
        if (!isEnableWindowPolicy()) {
            return false;
        }
        return this.mMultiTaskPolicyController.registerMultiTaskActionListener(listenerFlag, handler, listener);
    }

    public boolean unregisterMultiTaskActionListener(int listenerFlag, SmartPowerServiceInternal.MultiTaskActionListener listener) {
        if (!isEnableWindowPolicy()) {
            return false;
        }
        return this.mMultiTaskPolicyController.unregisterMultiTaskActionListener(listenerFlag, listener);
    }

    public void onMultiTaskActionStart(MultiTaskActionManager.ActionInfo actionInfo, int callingPid) {
        if (isEnableWindowPolicy()) {
            if (actionInfo == null) {
                Slog.w(TAG, "action info must not be null");
                return;
            }
            if (DEBUG) {
                Slog.d(TAG, "onMultiTaskActionStart: " + actionInfo);
            }
            String typeString = MultiTaskActionManager.actionTypeToString(actionInfo.getType());
            Trace.asyncTraceBegin(32L, "Multi task " + typeString, 0);
            actionInfo.setCallingPid(callingPid);
            onMultiTaskActionChanged(1, actionInfo);
        }
    }

    public void onMultiTaskActionEnd(MultiTaskActionManager.ActionInfo actionInfo, int callingPid) {
        if (isEnableWindowPolicy()) {
            if (actionInfo == null) {
                Slog.w(TAG, "action info must not be null");
                return;
            }
            if (DEBUG) {
                Slog.d(TAG, "onMultiTaskActionEnd: " + actionInfo);
            }
            String typeString = MultiTaskActionManager.actionTypeToString(actionInfo.getType());
            Trace.asyncTraceEnd(64L, "Multi task " + typeString, 0);
            actionInfo.setCallingPid(callingPid);
            onMultiTaskActionChanged(2, actionInfo);
        }
    }

    private void onMultiTaskActionChanged(int event, MultiTaskActionManager.ActionInfo actionInfo) {
        Message message = this.mHandler.obtainMessage(1);
        message.arg1 = event;
        message.obj = actionInfo;
        this.mHandler.sendMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyMultiTaskActionChanged(int event, MultiTaskActionManager.ActionInfo actionInfo) {
        this.mMultiTaskPolicyController.notifyMultiTaskActionChanged(event, actionInfo);
    }

    private boolean isEnableWindowPolicy() {
        return sEnable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int event = msg.arg1;
                    MultiTaskActionManager.ActionInfo actionInfo = (MultiTaskActionManager.ActionInfo) msg.obj;
                    SmartWindowPolicyManager.this.notifyMultiTaskActionChanged(event, actionInfo);
                    return;
                default:
                    return;
            }
        }
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        pw.println("SmartWindowPolicyManager Dump:");
        pw.println("Enable: " + isEnableWindowPolicy());
        MultiTaskPolicyController multiTaskPolicyController = this.mMultiTaskPolicyController;
        if (multiTaskPolicyController != null) {
            multiTaskPolicyController.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MultiTaskPolicyController {
        private final Object mLock;
        private final SparseArray<MultiTaskActionListenerDelegate> mMultiTaskActionListeners;

        private MultiTaskPolicyController() {
            this.mLock = new Object();
            this.mMultiTaskActionListeners = new SparseArray<>(4);
        }

        public boolean registerMultiTaskActionListener(int listenerFlag, Handler handler, SmartPowerServiceInternal.MultiTaskActionListener listener) {
            synchronized (this.mLock) {
                if (!isValidMultiTaskListenerFlag(listenerFlag)) {
                    Slog.d(SmartWindowPolicyManager.TAG, "the listener flag is invalid.");
                    return false;
                }
                if (handler != null && listener != null) {
                    Looper looper = handler.getLooper();
                    if (looper == null) {
                        Slog.d(SmartWindowPolicyManager.TAG, "unable to obtain the loop corresponding to the handler.");
                        return false;
                    }
                    MultiTaskActionListenerDelegate listenerDelegate = new MultiTaskActionListenerDelegate(listenerFlagToString(listenerFlag), looper, listener);
                    this.mMultiTaskActionListeners.put(listenerFlag, listenerDelegate);
                    return true;
                }
                Slog.d(SmartWindowPolicyManager.TAG, "listener or handler must not be null.");
                return false;
            }
        }

        public boolean unregisterMultiTaskActionListener(int listenerFlag, SmartPowerServiceInternal.MultiTaskActionListener listener) {
            synchronized (this.mLock) {
                if (!isValidMultiTaskListenerFlag(listenerFlag)) {
                    Slog.d(SmartWindowPolicyManager.TAG, "the listener flag is invalid.");
                    return false;
                }
                if (listener == null) {
                    Slog.d(SmartWindowPolicyManager.TAG, "listener must not be null.");
                    return false;
                }
                MultiTaskActionListenerDelegate listenerDelegate = this.mMultiTaskActionListeners.get(listenerFlag);
                if (listenerDelegate == null) {
                    return false;
                }
                if (listenerDelegate.getListener() != listener) {
                    Slog.d(SmartWindowPolicyManager.TAG, "the listener flag is not match the listener.");
                    return false;
                }
                listenerDelegate.clearEvents();
                this.mMultiTaskActionListeners.remove(listenerFlag);
                return true;
            }
        }

        public void notifyMultiTaskActionChanged(int event, MultiTaskActionManager.ActionInfo actionInfo) {
            int[] tids = actionInfo.getDrawnTids();
            int callingPid = actionInfo.getCallingPid();
            if (event == 1) {
                SmartWindowPolicyManager.this.mSmartDisplayPolicyManager.notifyMultiTaskActionStart(actionInfo);
                SmartWindowPolicyManager.this.mSmartBoostPolicyManager.beginSchedThreads(tids, callingPid, 1);
            } else if (event == 2) {
                SmartWindowPolicyManager.this.mSmartDisplayPolicyManager.notifyMultiTaskActionEnd(actionInfo);
                SmartWindowPolicyManager.this.mSmartBoostPolicyManager.stopCurrentSchedBoost(tids, callingPid, 1);
            }
            dispatchMultiTaskActionEvent(event, actionInfo);
        }

        public void dispatchMultiTaskActionEvent(int event, MultiTaskActionManager.ActionInfo actionInfo) {
            synchronized (this.mLock) {
                int numListeners = this.mMultiTaskActionListeners.size();
                for (int i = 0; i < numListeners; i++) {
                    this.mMultiTaskActionListeners.valueAt(i).sendMultiTaskActionEvent(event, actionInfo);
                }
            }
        }

        private boolean isValidMultiTaskListenerFlag(int listenerFlag) {
            return listenerFlag > 0 && listenerFlag < 3;
        }

        private String listenerFlagToString(int listenerFlag) {
            switch (listenerFlag) {
                case 1:
                    return "sched boost";
                case 2:
                    return "multi scene";
                default:
                    return "unknown listener";
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class MultiTaskActionListenerDelegate extends Handler {
            private SmartPowerServiceInternal.MultiTaskActionListener mListener;
            private String mSourceName;

            public MultiTaskActionListenerDelegate(String sourceName, Looper looper, SmartPowerServiceInternal.MultiTaskActionListener listener) {
                super(looper);
                this.mSourceName = null;
                this.mListener = null;
                this.mSourceName = sourceName;
                this.mListener = listener;
            }

            public void sendMultiTaskActionEvent(int event, MultiTaskActionManager.ActionInfo actionInfo) {
                Message msg = obtainMessage(event, actionInfo);
                sendMessage(msg);
            }

            public void clearEvents() {
                removeCallbacksAndMessages(null);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        MultiTaskActionManager.ActionInfo actionInfo = (MultiTaskActionManager.ActionInfo) msg.obj;
                        this.mListener.onMultiTaskActionStart(actionInfo);
                        return;
                    case 2:
                        MultiTaskActionManager.ActionInfo actionInfo2 = (MultiTaskActionManager.ActionInfo) msg.obj;
                        this.mListener.onMultiTaskActionEnd(actionInfo2);
                        return;
                    default:
                        return;
                }
            }

            public String getSourceName() {
                return this.mSourceName;
            }

            public SmartPowerServiceInternal.MultiTaskActionListener getListener() {
                return this.mListener;
            }
        }

        public void dump(PrintWriter pw) {
            pw.println("MultiTaskPolicyController Dump:");
            pw.println("    Multi-Task Action Listeners:");
            if (this.mMultiTaskActionListeners.size() == 0) {
                pw.println("    None");
                return;
            }
            for (int i = 0; i < this.mMultiTaskActionListeners.size(); i++) {
                pw.printf("     [%d]: %s\n", Integer.valueOf(i), this.mMultiTaskActionListeners.valueAt(i).getSourceName());
            }
        }
    }
}
