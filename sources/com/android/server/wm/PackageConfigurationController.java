package com.android.server.wm;

import android.content.Context;
import android.os.Process;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class PackageConfigurationController extends Thread {
    private static final String COMMAND_OPTION_FORCE_UPDATE = "ForceUpdate";
    private static final String COMMAND_OPTION_POLICY_RESET = "PolicyReset";
    private static final String PACKAGE_CONFIGURATION_COMMAND = "-packageconfiguration";
    private static final String PREFIX_ACTION_POLICY_UPDATED = "sec.app.policy.UPDATE.";
    private static final String SET_POLICY_DISABLED_COMMAND = "-setPolicyDisabled";
    private static final String TAG = "PackageConfigurationController";
    final ActivityTaskManagerService mAtmService;
    private final Context mContext;
    private final ArrayList<String> mLogs;
    boolean mPolicyDisabled;
    private final Map<String, PolicyImpl> mPolicyImplMap;
    private final Set<String> mPolicyRequestQueue;
    private final Set<String> mTmpPolicyRequestQueue;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageConfigurationController(ActivityTaskManagerService atmService) {
        super("PackageConfigurationUpdateThread");
        this.mLogs = new ArrayList<>();
        this.mPolicyImplMap = new ConcurrentHashMap();
        this.mPolicyRequestQueue = new HashSet();
        this.mTmpPolicyRequestQueue = new HashSet();
        this.mAtmService = atmService;
        this.mContext = atmService.mContext;
    }

    private void initialize() {
        Slog.d(TAG, "initialize");
        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PackageConfigurationController.this.lambda$initialize$0((String) obj, (PolicyImpl) obj2);
            }
        });
        scheduleUpdatePolicyItem(null, 300000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initialize$0(String str, PolicyImpl policy) {
        policy.init();
        scheduleUpdatePolicyItem(PREFIX_ACTION_POLICY_UPDATED + str, 0L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean executeShellCommand(String command, String[] args, final PrintWriter pw) {
        if (!MiuiAppSizeCompatModeStub.get().isEnabled()) {
            Slog.d(TAG, "MiuiAppSizeCompatMode not enabled");
            return false;
        }
        synchronized (this) {
            if (PACKAGE_CONFIGURATION_COMMAND.equals(command)) {
                if (args.length == 1) {
                    pw.println();
                    if (COMMAND_OPTION_FORCE_UPDATE.equals(args[0])) {
                        pw.println("Started the update.");
                        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda2
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                PackageConfigurationController.lambda$executeShellCommand$1(pw, (String) obj, (PolicyImpl) obj2);
                            }
                        });
                    }
                }
                return true;
            }
            if (SET_POLICY_DISABLED_COMMAND.equals(command)) {
                if (args.length == 1) {
                    if (args[0] == null) {
                        return true;
                    }
                    boolean newPolicyDisabled = Boolean.parseBoolean(args[0]);
                    if (this.mPolicyDisabled != newPolicyDisabled) {
                        this.mPolicyDisabled = newPolicyDisabled;
                        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda3
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                ((PolicyImpl) obj2).propagateToCallbacks();
                            }
                        });
                    }
                }
                return true;
            }
            for (Object item : this.mPolicyImplMap.values()) {
                if (((PolicyImpl) item).executeShellCommandLocked(command, args, pw)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$executeShellCommand$1(PrintWriter pw, String str, PolicyImpl policy) {
        policy.updatePolicyItem(true);
        pw.println(str + " update forced.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPolicy(PolicyImpl impl) {
        this.mPolicyImplMap.put(impl.getPolicyName(), impl);
        this.mTmpPolicyRequestQueue.add(impl.getPolicyName());
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(10);
        initialize();
        synchronized (this) {
            while (true) {
                try {
                    try {
                        for (String str : this.mPolicyRequestQueue) {
                            PolicyImpl policyImpl = this.mPolicyImplMap.get(str);
                            if (policyImpl != null) {
                                policyImpl.updatePolicyItem(false);
                            }
                        }
                        this.mPolicyRequestQueue.clear();
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleUpdatePolicyItem(String policyRequest, long delayMillis) {
        if (policyRequest != null) {
            this.mTmpPolicyRequestQueue.add(policyRequest);
        }
        this.mAtmService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PackageConfigurationController.this.lambda$scheduleUpdatePolicyItem$3();
            }
        }, delayMillis);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scheduleUpdatePolicyItem$3() {
        synchronized (this) {
            try {
                this.mPolicyRequestQueue.addAll(this.mTmpPolicyRequestQueue);
                this.mTmpPolicyRequestQueue.clear();
                notifyAll();
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startThread() {
        if (this.mPolicyImplMap.isEmpty()) {
            return;
        }
        start();
    }
}
