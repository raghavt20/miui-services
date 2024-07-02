package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.multisence.MultiSenceConfig;
import com.miui.server.multisence.MultiSenceDynamicController;
import com.miui.server.multisence.MultiSenceManagerInternal;
import com.miui.server.multisence.MultiSenceService;
import com.miui.server.multisence.MultiSenceServiceUtils;
import com.miui.server.multisence.SingleWindowInfo;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.Map;

@MiuiStubHead(manifestName = "com.android.server.wm.MultiSenceManagerInternalStub$$")
/* loaded from: classes.dex */
public class MultiSenceManagerInternalStubImpl implements MultiSenceManagerInternalStub {
    private static final String ACTION_MULTISENCE_FOCUSED_CHANGE = "com.xiaomi.multisence.action.FOCUSED_CHANGE";
    private static final String EXTRA_MULTISENCE_FOCUSED_PACKAGE = "com.xiaomi.multisence.extra.FOCUSED_PACKAGE";
    private static final String TAG = "MultiSenceManagerInternalStubImpl";
    private Context mContext;
    public Handler mHandler;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    private static MultiSenceManagerInternal mMultiSenceMI = null;
    private static WindowManagerService service = null;
    private WindowState mCurrentFocusWindow = null;
    private boolean mBootComplete = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MultiSenceManagerInternalStubImpl> {

        /* compiled from: MultiSenceManagerInternalStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MultiSenceManagerInternalStubImpl INSTANCE = new MultiSenceManagerInternalStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MultiSenceManagerInternalStubImpl m2706provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MultiSenceManagerInternalStubImpl m2705provideNewInstance() {
            return new MultiSenceManagerInternalStubImpl();
        }
    }

    public void bootComplete() {
        this.mBootComplete = true;
    }

    public void init(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public boolean systemReady() {
        mMultiSenceMI = (MultiSenceManagerInternal) ServiceManager.getService(MultiSenceService.SERVICE_NAME);
        WindowManagerService service2 = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        service = service2;
        if (mMultiSenceMI == null || service2 == null) {
            Slog.e(TAG, "MultiSenceManagerInternalStubImpl does not connect to miui_multi_sence or WMS");
            return false;
        }
        return true;
    }

    /* loaded from: classes.dex */
    class UpdateScreenStatus implements Runnable {
        private WindowState mNewFocus;

        public UpdateScreenStatus(WindowState newFocus) {
            this.mNewFocus = newFocus;
        }

        @Override // java.lang.Runnable
        public void run() {
            Map<String, SingleWindowInfo> sWindows;
            if (MultiSenceManagerInternalStubImpl.mMultiSenceMI == null) {
                MultiSenceManagerInternalStubImpl.this.LOG_IF_DEBUG("not connect to multi-sence core service");
                return;
            }
            if (!MultiSenceDynamicController.IS_CLOUD_ENABLED) {
                MultiSenceManagerInternalStubImpl.this.LOG_IF_DEBUG("func not enable due to Cloud Controller");
                return;
            }
            if (this.mNewFocus == null) {
                MultiSenceManagerInternalStubImpl.this.LOG_IF_DEBUG("new focus is null");
                return;
            }
            if (MultiSenceManagerInternalStubImpl.this.mContext != null && MultiSenceManagerInternalStubImpl.this.mBootComplete) {
                MultiSenceManagerInternalStubImpl.this.sendFocusWindowsBroadcast(this.mNewFocus);
            }
            if (!MultiSenceManagerInternalStubImpl.this.isWindowPackageChanged(this.mNewFocus)) {
                MultiSenceManagerInternalStubImpl.this.mCurrentFocusWindow = this.mNewFocus;
                return;
            }
            MultiSenceManagerInternalStubImpl.this.mCurrentFocusWindow = this.mNewFocus;
            boolean connection = MultiSenceManagerInternalStubImpl.this.isConnectToWMS();
            if (connection) {
                synchronized (MultiSenceManagerInternalStubImpl.service.getGlobalLock()) {
                    Trace.traceBegin(32L, "updateScreenStatusWithFoucs");
                    sWindows = MultiSenceManagerInternalStubImpl.this.getWindowsNeedToSched();
                    if (sWindows != null && sWindows.containsKey(this.mNewFocus.getOwningPackage())) {
                        sWindows.get(this.mNewFocus.getOwningPackage()).setFocused(true);
                    }
                    Trace.traceEnd(32L);
                }
                synchronized (MultiSenceManagerInternalStubImpl.mMultiSenceMI) {
                    MultiSenceManagerInternalStubImpl.mMultiSenceMI.setUpdateReason("focus");
                    MultiSenceManagerInternalStubImpl.mMultiSenceMI.updateStaticWindowsInfo(sWindows);
                }
            }
        }
    }

    public void updateScreenStatusWithFoucs(WindowState newFocus) {
        UpdateScreenStatus runnable = new UpdateScreenStatus(newFocus);
        this.mHandler.post(runnable);
    }

    public void sendFocusWindowsBroadcast(WindowState window) {
        Intent intent = new Intent(ACTION_MULTISENCE_FOCUSED_CHANGE);
        String focusPackage = window.getOwningPackage();
        if (focusPackage == null) {
            Slog.i(TAG, "focusPackage为null");
            intent.putExtra(EXTRA_MULTISENCE_FOCUSED_PACKAGE, "NO_PACKAGE");
            this.mContext.sendBroadcast(intent);
        } else {
            Slog.i(TAG, "packageName = " + focusPackage);
            intent.putExtra(EXTRA_MULTISENCE_FOCUSED_PACKAGE, focusPackage);
            this.mContext.sendBroadcast(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isConnectToWMS() {
        if (service == null) {
            Slog.e(TAG, "MultiSenceManagerInternalStubImpl does not connect to WMS");
            return false;
        }
        return true;
    }

    public Map<String, SingleWindowInfo> getWindowsNeedToSched() {
        return MultiSenceUtils.getInstance().getWindowsNeedToSched();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWindowPackageChanged(WindowState newFocus) {
        if (newFocus == null) {
            return false;
        }
        if (this.mCurrentFocusWindow == null) {
            return true;
        }
        return !newFocus.getOwningPackage().equals(this.mCurrentFocusWindow.getOwningPackage());
    }

    public boolean checkFWSupport(String packageName) {
        boolean z = true;
        if (!MultiSenceConfig.FW_ENABLE) {
            if (!MultiSenceConfig.DEBUG_FW && !MultiSenceConfig.DEBUG_CONFIG) {
                z = false;
            }
            MultiSenceServiceUtils.msLogD(z, "floating window is not supported by multisence config");
            return false;
        }
        if (packageName == null || packageName.length() == 0) {
            return false;
        }
        synchronized (MultiSenceConfig.getInstance().floatingWindowWhiteList) {
            if (MultiSenceConfig.getInstance().floatingWindowWhiteList.contains(packageName)) {
                MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_FW, "floating window is supported");
                return true;
            }
            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_FW, "floating window is not supported");
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void LOG_IF_DEBUG(String log) {
        if (DEBUG) {
            Slog.d(TAG, log);
        }
    }
}
