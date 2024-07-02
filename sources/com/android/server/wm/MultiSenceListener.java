package com.android.server.wm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.server.multisence.MultiSenceConfig;
import com.miui.server.multisence.MultiSenceDynamicController;
import com.miui.server.multisence.MultiSenceManagerInternal;
import com.miui.server.multisence.MultiSenceService;
import com.miui.server.multisence.SingleWindowInfo;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.abtest.d.d;
import java.util.Map;
import miui.smartpower.MultiTaskActionManager;

/* loaded from: classes.dex */
public class MultiSenceListener implements SmartPowerServiceInternal.MultiTaskActionListener {
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    private static final int EVENT_MULTI_TASK_ACTION_END = 2;
    private static final int EVENT_MULTI_TASK_ACTION_START = 1;
    private static final int MSG_DISPATCH_MULTI_TASK = 1;
    private static final String TAG = "MultiSenceListener";
    private static WindowManagerService service;
    private boolean isSystemReady = false;
    private final H mHandler;
    private MultiSenceManagerInternal mMultiSenceMI;

    public MultiSenceListener(Looper looper) {
        LOG_IF_DEBUG("MultiSenceListener init");
        this.mHandler = new H(looper);
    }

    public boolean systemReady() {
        this.mMultiSenceMI = (MultiSenceManagerInternal) ServiceManager.getService(MultiSenceService.SERVICE_NAME);
        WindowManagerService service2 = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        service = service2;
        if (this.mMultiSenceMI == null || service2 == null) {
            Slog.e(TAG, "MultiSence Listener does not connect to miui_multi_sence or WMS");
            return false;
        }
        this.isSystemReady = true;
        return true;
    }

    private boolean readyToWork() {
        if (!this.isSystemReady) {
            return false;
        }
        if (this.mMultiSenceMI == null) {
            LOG_IF_DEBUG("not connect to multi-sence core service");
            return false;
        }
        if (!MultiSenceDynamicController.IS_CLOUD_ENABLED) {
            LOG_IF_DEBUG("func not enable due to Cloud Contrller");
            return false;
        }
        if (!MultiSenceConfig.MULTITASK_ANIM_SCHED_ENABLED) {
            LOG_IF_DEBUG("multi-task animation not enable by config");
            return false;
        }
        return true;
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal.MultiTaskActionListener
    public void onMultiTaskActionStart(MultiTaskActionManager.ActionInfo actionInfo) {
        if (!readyToWork()) {
            return;
        }
        if (actionInfo == null) {
            Slog.w(TAG, "action info must not be null");
            return;
        }
        Trace.traceBegin(32L, "MultisenceListener.onMultiTaskActionStart");
        onMultiTaskActionChanged(1, actionInfo);
        Trace.traceEnd(32L);
    }

    @Override // com.miui.app.smartpower.SmartPowerServiceInternal.MultiTaskActionListener
    public void onMultiTaskActionEnd(MultiTaskActionManager.ActionInfo actionInfo) {
        if (!readyToWork()) {
            return;
        }
        if (actionInfo == null) {
            Slog.w(TAG, "action info must not be null");
            return;
        }
        Trace.traceBegin(32L, "MultisenceListener.onMultiTaskActionEnd");
        onMultiTaskActionChanged(2, actionInfo);
        Trace.traceEnd(32L);
    }

    private void onMultiTaskActionChanged(int event, MultiTaskActionManager.ActionInfo actionInfo) {
        Message message = this.mHandler.obtainMessage(event);
        message.obj = actionInfo;
        if (event == 2 && actionInfo.getType() == 4097) {
            this.mHandler.sendMessageDelayed(message, 800L);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            MultiTaskActionManager.ActionInfo taskInfo = (MultiTaskActionManager.ActionInfo) msg.obj;
            switch (msg.what) {
                case 1:
                    String actionDetail = "EVENT_MULTI_TASK_ACTION_START: " + MultiTaskActionManager.actionTypeToString(taskInfo.getType());
                    Trace.traceBegin(32L, actionDetail);
                    MultiSenceListener.this.LOG_IF_DEBUG(actionDetail);
                    MultiSenceListener.this.updateDynamicSenceInfo(taskInfo, true);
                    Trace.traceEnd(32L);
                    return;
                case 2:
                    String actionEndDetail = "EVENT_MULTI_TASK_ACTION_END: " + MultiTaskActionManager.actionTypeToString(taskInfo.getType());
                    Trace.traceBegin(32L, actionEndDetail);
                    MultiSenceListener.this.LOG_IF_DEBUG(actionEndDetail);
                    MultiSenceListener.this.updateDynamicSenceInfo(taskInfo, false);
                    MultiSenceListener.this.updateScreenStatus();
                    Trace.traceEnd(32L);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDynamicSenceInfo(MultiTaskActionManager.ActionInfo taskInfo, boolean isStarted) {
        String tids_s = d.h;
        for (int value : taskInfo.getDrawnTids()) {
            tids_s = tids_s + String.valueOf(value) + d.h;
        }
        LOG_IF_DEBUG("Task uid: " + taskInfo.getUid() + ", Task tid: " + tids_s);
        int senceId = taskInfo.getType();
        int uid = taskInfo.getUid();
        int[] tids = taskInfo.getDrawnTids();
        this.mMultiSenceMI.updateDynamicWindowsInfo(senceId, uid, tids, isStarted);
    }

    public void updateScreenStatus() {
        Map<String, SingleWindowInfo> sWindows;
        if (this.mMultiSenceMI == null || !MultiSenceDynamicController.IS_CLOUD_ENABLED) {
            Slog.w(TAG, "return updateScreenStatus");
            return;
        }
        boolean connection = isConnectToWMS();
        if (connection) {
            synchronized (service.getGlobalLock()) {
                Trace.traceBegin(32L, "getWindowsNeedToSched from multi-stack sence");
                sWindows = getWindowsNeedToSched();
                Trace.traceEnd(32L);
                DisplayContent displayContent = service.mRoot.getTopFocusedDisplayContent();
                WindowState foucsWindow = displayContent.mCurrentFocus;
                try {
                    sWindows.get(foucsWindow.getOwningPackage()).setFocused(true);
                } catch (NullPointerException npe) {
                    Slog.e(TAG, "setFocus error: " + npe);
                    return;
                }
            }
            synchronized (this.mMultiSenceMI) {
                this.mMultiSenceMI.setUpdateReason("multi-task");
                this.mMultiSenceMI.updateStaticWindowsInfo(sWindows);
            }
        }
    }

    private boolean isConnectToWMS() {
        if (service == null) {
            Slog.e(TAG, "MultiSence Listener does not connect to WMS");
            return false;
        }
        return true;
    }

    private Map<String, SingleWindowInfo> getWindowsNeedToSched() {
        return MultiSenceUtils.getInstance().getWindowsNeedToSched();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void LOG_IF_DEBUG(String log) {
        if (DEBUG) {
            Slog.d(TAG, log);
        }
    }
}
