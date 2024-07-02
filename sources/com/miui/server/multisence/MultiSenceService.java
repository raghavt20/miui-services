package com.miui.server.multisence;

import android.content.Context;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.system.Os;
import android.util.Slog;
import android.view.IFloatingWindow;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.wm.MultiSenceListener;
import com.android.server.wm.MultiSenceManagerInternalStub;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.server.multisence.SingleWindowInfo;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import miui.smartpower.MultiTaskActionManager;

/* loaded from: classes.dex */
public class MultiSenceService extends SystemService {
    private static final String DEFAULT_MULTISENCE_FREEFORM_PACKAGE = "com.android.systemui";
    private static final int DELAY_MS = 50;
    private static final String MCD_DF_PATH = "/data/system/mcd/mwdf";
    public static final String SERVICE_NAME = "miui_multi_sence";
    private static final String TAG = "MultiSenceService";
    private BinderService mBinderService;
    private Context mContext;
    private MultiSenceDynamicController mController;
    public Handler mHandler;
    MultiSenceListener mMultiSenceListener;
    private Map<String, SingleWindowInfo> mNewWindows;
    private MultiSenceServicePolicy mPolicy;
    private String mReason;
    private final SmartPowerServiceInternal mSmartPowerService;
    private ServiceThread mThread;
    private final Runnable mUpdateStaticWindows;
    private Map<String, SingleWindowInfo> mWindows;
    private Map<Integer, String> multiSencePackages;
    private MultiTaskActionManager multiTaskActionManager;
    public static boolean IS_SERVICE_ENABLED = SystemProperties.getBoolean(MultiSenceConfig.PROP_MULTISENCE_ENABLE, true);
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    private static boolean mMultiTaskActionListenerRegistered = false;

    public MultiSenceService(Context context) {
        super(context);
        this.mWindows = new HashMap();
        this.mNewWindows = null;
        this.multiSencePackages = new HashMap();
        this.mPolicy = null;
        this.mBinderService = null;
        this.mSmartPowerService = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
        this.multiTaskActionManager = new MultiTaskActionManager();
        this.mReason = "unknown";
        this.mUpdateStaticWindows = new Runnable() { // from class: com.miui.server.multisence.MultiSenceService.1
            @Override // java.lang.Runnable
            public void run() {
                MultiSenceService.this.updateStaticWindowsInfo();
            }
        };
        this.mContext = context;
        this.mPolicy = new MultiSenceServicePolicy();
        this.mThread = MultiSenceThread.getInstance();
        this.mHandler = new H(this.mThread.getLooper());
        this.mMultiSenceListener = new MultiSenceListener(this.mThread.getLooper());
        MultiSenceConfig.getInstance().init(this, this.mContext);
        createFileDevice(MCD_DF_PATH);
        registerMultiTaskActionListener();
        MultiSenceDynamicController multiSenceDynamicController = new MultiSenceDynamicController(this, this.mHandler, this.mContext);
        this.mController = multiSenceDynamicController;
        multiSenceDynamicController.registerCloudObserver();
        this.mController.registerVRSCloudObserver();
        this.mController.registerFWCloudObserver();
        this.multiSencePackages.put(4097, "com.android.systemui");
        this.multiSencePackages.put(4101, "com.android.systemui");
        this.multiSencePackages.put(4102, "com.android.systemui");
        this.multiSencePackages.put(4100, "com.android.systemui");
        this.multiSencePackages.put(8193, "com.android.systemui");
        this.multiSencePackages.put(16385, "com.android.systemui");
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [com.miui.server.multisence.MultiSenceService$BinderService, android.os.IBinder] */
    public void onStart() {
        ?? binderService = new BinderService();
        this.mBinderService = binderService;
        publishBinderService(SERVICE_NAME, binderService);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            LOG_IF_DEBUG("System ready: 500");
            MultiSenceConfig.getInstance().updateSubFuncStatus();
            this.mMultiSenceListener.systemReady();
            MultiSenceManagerInternalStub.getInstance().systemReady();
            MultiSenceManagerInternalStub.getInstance().init(this.mContext, this.mHandler);
            return;
        }
        if (phase == 1000) {
            MultiSenceManagerInternalStub.getInstance().bootComplete();
            MultiSenceConfig.getInstance().initVRSWhiteList();
            MultiSenceConfig.getInstance().initFWWhiteList();
        }
    }

    private void createFileDevice(String name) {
        StringBuilder sb;
        try {
            try {
                File mwdf_file = new File(name);
                if (!mwdf_file.exists()) {
                    mwdf_file.createNewFile();
                }
                Os.chmod(name, 438);
                sb = new StringBuilder();
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create: " + name + ", error: " + e.toString());
                sb = new StringBuilder();
            }
            Slog.e(TAG, sb.append("Create ending: ").append(name).toString());
        } catch (Throwable th) {
            Slog.e(TAG, "Create ending: " + name);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void registerMultiTaskActionListener() {
        if (!MultiSenceDynamicController.IS_CLOUD_ENABLED) {
            Slog.i(TAG, "need not to register multisence listener due to cloud controller.");
            return;
        }
        if (mMultiTaskActionListenerRegistered) {
            Slog.i(TAG, "Listener has been registered. Do not register again.");
            return;
        }
        boolean isRegisterSuccess = this.mSmartPowerService.registerMultiTaskActionListener(2, this.mHandler, this.mMultiSenceListener);
        if (isRegisterSuccess) {
            mMultiTaskActionListenerRegistered = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void unregisterMultiTaskActionListener() {
        if (!mMultiTaskActionListenerRegistered) {
            Slog.i(TAG, "Listener has not been registered. Do need to unregister.");
            return;
        }
        if (MultiSenceDynamicController.IS_CLOUD_ENABLED) {
            Slog.i(TAG, "need not to unregister multisence listener due to cloud controller.");
            return;
        }
        boolean isUnRegisterSuccess = this.mSmartPowerService.unregisterMultiTaskActionListener(2, this.mMultiSenceListener);
        if (isUnRegisterSuccess) {
            mMultiTaskActionListenerRegistered = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearServiceStatus() {
        synchronized (this) {
            this.mPolicy.reset();
            this.mWindows.clear();
        }
    }

    /* loaded from: classes.dex */
    static class MultiSenceThread extends ServiceThread {
        private static MultiSenceThread sInstance;

        private MultiSenceThread() {
            super(MultiSenceConfig.SERVICE_JAVA_NAME, -2, true);
        }

        private static void ensureThreadLocked() {
            if (sInstance == null) {
                MultiSenceThread multiSenceThread = new MultiSenceThread();
                sInstance = multiSenceThread;
                multiSenceThread.start();
            }
        }

        public static MultiSenceThread getInstance() {
            MultiSenceThread multiSenceThread;
            synchronized (MultiSenceThread.class) {
                ensureThreadLocked();
                multiSenceThread = sInstance;
            }
            return multiSenceThread;
        }
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int uid = msg.arg1;
            boolean isStarted = msg.arg2 != 0;
            int[] tids = (int[]) msg.obj;
            switch (msg.what) {
                case 4097:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedFreeformActionMove(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(4097));
                    return;
                case 4100:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedFreeformActionResize(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(4100));
                    return;
                case 4101:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedFreeformActionEnterFullScreen(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(4101));
                    return;
                case 4102:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedFreeformActionElude(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(4102));
                    return;
                case 8193:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedSplitscreenctionDivider(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(8193));
                    return;
                case 16384:
                    return;
                case 16385:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedMWSActionMove(uid, tids, isStarted, (String) MultiSenceService.this.multiSencePackages.get(16385));
                    return;
                case 20481:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedFloatingWindowMove(uid, tids, isStarted);
                    return;
                default:
                    MultiSenceService.this.mPolicy.dynamicSenceSchedDefault(uid, tids, isStarted, null);
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IFloatingWindow.Stub implements MultiSenceManagerInternal {
        private BinderService() {
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(MultiSenceService.this.mContext, MultiSenceService.TAG, pw)) {
                pw.println("multisence (miui_multi_sence):");
                try {
                    if (args.length <= 0) {
                        MultiSenceConfig.getInstance().dumpConfig(pw);
                    }
                    if ("config".equals(args[0])) {
                        MultiSenceConfig.getInstance().dumpConfig(pw);
                    }
                    pw.println("multisence dump end");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            MultiSenceService multiSenceService = MultiSenceService.this;
            new MultiSenceShellCmd(multiSenceService).exec(this, in, out, err, args, callback, resultReceiver);
        }

        @Override // com.miui.server.multisence.MultiSenceManagerInternal
        public void updateStaticWindowsInfo(Map<String, SingleWindowInfo> sWindows) {
            MultiSenceService.this.updateStaticWindowsInfo(sWindows);
        }

        @Override // com.miui.server.multisence.MultiSenceManagerInternal
        public void updateDynamicWindowsInfo(int i, int i2, int[] iArr, boolean z) {
            Message obtainMessage = MultiSenceService.this.mHandler.obtainMessage(i);
            obtainMessage.arg1 = i2;
            obtainMessage.arg2 = z ? 1 : 0;
            obtainMessage.obj = iArr;
            MultiSenceService.this.mHandler.sendMessage(obtainMessage);
        }

        @Override // com.miui.server.multisence.MultiSenceManagerInternal
        public void showAllWindowInfo(String reason) {
            MultiSenceService.this.showAllWindowInfo(reason);
        }

        @Override // com.miui.server.multisence.MultiSenceManagerInternal
        public void setUpdateReason(String reason) {
            MultiSenceService.this.setUpdateReason(reason);
        }

        public void floatingWindowShow(int[] req_tids, String packageName, Rect bounds) {
            MultiSenceService.this.floatingWindowShow(req_tids, packageName, bounds);
        }

        public void floatingWindowClose(int[] req_tids, String packageName) {
            MultiSenceService.this.floatingWindowClose(req_tids, packageName);
        }

        public void floatingWindowMoveStart(int[] req_tids, String packageName) {
            MultiSenceService.this.floatingWindowMoveStart(req_tids, packageName);
        }

        public void floatingWindowMoveEnd(int[] req_tids, String packageName, Rect bounds) {
            MultiSenceService.this.floatingWindowMoveEnd(req_tids, packageName, bounds);
        }
    }

    public void setUpdateReason(String reason) {
        this.mReason = reason;
    }

    public void floatingWindowShow(int[] req_tids, String packageName, Rect bounds) {
        notifyMultiTaskActionEnd(req_tids, packageName, bounds, 20482);
    }

    public void floatingWindowClose(int[] req_tids, String packageName) {
        notifyMultiTaskActionEnd(req_tids, packageName, null, 20483);
    }

    public void floatingWindowMoveStart(int[] req_tids, String packageName) {
        long origId = Binder.clearCallingIdentity();
        this.multiTaskActionManager.notifyMultiTaskActionStart(new MultiTaskActionManager.ActionInfo(20481, Process.myUid(), req_tids));
        Binder.restoreCallingIdentity(origId);
    }

    public void floatingWindowMoveEnd(int[] req_tids, String packageName, Rect bounds) {
        notifyMultiTaskActionEnd(req_tids, packageName, bounds, 20481);
    }

    public void notifyMultiTaskActionEnd(int[] req_tids, String packageName, Rect bounds, int senceId) {
        long origId = Binder.clearCallingIdentity();
        MultiTaskActionManager.ActionInfo floatingWindowInfo = new MultiTaskActionManager.ActionInfo(senceId, Process.myUid(), req_tids);
        this.multiTaskActionManager.notifyMultiTaskActionEnd(floatingWindowInfo);
        Binder.restoreCallingIdentity(origId);
    }

    public void updateStaticWindowsInfo(Map<String, SingleWindowInfo> inWindows) {
        this.mHandler.removeCallbacks(this.mUpdateStaticWindows);
        this.mNewWindows = inWindows;
        this.mHandler.postDelayed(this.mUpdateStaticWindows, 50L);
    }

    public void updateStaticWindowsInfo() {
        Iterator<String> it = this.mNewWindows.keySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String key = it.next();
            SingleWindowInfo window = this.mNewWindows.get(key);
            if (window == null) {
                Slog.w(TAG, "window {" + key + "} in null");
            } else if (window.isInputFocused()) {
                String newInputFocus = window.getPackageName();
                this.mPolicy.updateInputFocus(newInputFocus);
                break;
            }
        }
        if (!MultiSenceDynamicController.IS_CLOUD_ENABLED) {
            LOG_IF_DEBUG("Cloud Controller refuses.");
            return;
        }
        if (!isScreenStateChanged(this.mNewWindows)) {
            showAllWindowInfo("not changed");
            return;
        }
        this.mPolicy.windowInfoPerPorcessing(this.mNewWindows);
        this.mPolicy.updateSchedPolicy(this.mWindows, this.mNewWindows);
        this.mPolicy.doSched();
        this.mWindows = this.mNewWindows;
        showAllWindowInfo(this.mReason);
    }

    private boolean isScreenStateChanged(Map<String, SingleWindowInfo> inWindows) {
        if (inWindows.size() != this.mWindows.size()) {
            return true;
        }
        for (String name_in : inWindows.keySet()) {
            if (!this.mWindows.containsKey(name_in)) {
                return true;
            }
            SingleWindowInfo inWindow = inWindows.get(name_in);
            SingleWindowInfo window = this.mWindows.get(name_in);
            if (inWindow.isInputFocused() != window.isInputFocused() || !inWindow.getWindowingModeString().equals(window.getWindowingModeString())) {
                return true;
            }
            if (window.isInFreeform() && inWindow.isInFreeform() && window.getWindowForm() != inWindow.getWindowForm()) {
                return true;
            }
            if (window.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM && inWindow.getWindowForm() == SingleWindowInfo.WindowForm.MUTIL_FREEDOM && !window.getRectValue().equals(inWindow.getRectValue())) {
                return true;
            }
        }
        return false;
    }

    public MultiSenceServicePolicy getPolicy() {
        return this.mPolicy;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyMultiSenceEnable(boolean isEnable) {
        this.mSmartPowerService.notifyMultiSenceEnable(isEnable);
    }

    public void showAllWindowInfo(String reason) {
        if (!DEBUG) {
            return;
        }
        int count = 0;
        LOG_IF_DEBUG("reason: " + reason);
        for (String key : this.mWindows.keySet()) {
            count++;
            LOG_IF_DEBUG("[" + count + "]: " + key + "-" + this.mWindows.get(key).getWindowingModeString() + "-" + this.mWindows.get(key).isInputFocused() + "-" + this.mWindows.get(key).isVisible());
        }
        LOG_IF_DEBUG("There are [" + count + "] windows in Screen");
    }

    /* loaded from: classes.dex */
    private class MultiSenceShellCmd extends ShellCommand {
        MultiSenceService mService;

        public MultiSenceShellCmd(MultiSenceService service) {
            this.mService = service;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case -2043320241:
                        if (cmd.equals("update-debug")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -133023812:
                        if (cmd.equals("update-status-sub-function")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3198785:
                        if (cmd.equals("help")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        onHelp();
                        return 0;
                    case 1:
                        MultiSenceConfig.getInstance().updateSubFuncStatus();
                        return 0;
                    case 2:
                        MultiSenceConfig.getInstance().updateDebugInfo();
                        return 0;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                pw.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e("ShellCommand", "Error running shell command", e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Multi-Sence Service Commands:");
            pw.println("  help");
        }
    }

    private void LOG_IF_DEBUG(String log) {
        if (DEBUG) {
            Slog.d(TAG, log);
        }
    }
}
