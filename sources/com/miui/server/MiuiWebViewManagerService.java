package com.miui.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.display.RampRateController;
import com.miui.app.am.ActivityManagerServiceProxy;
import com.miui.app.am.ProcessRecordProxy;
import java.util.ArrayList;
import java.util.List;
import miui.webview.IMiuiWebViewManager;

/* loaded from: classes.dex */
public class MiuiWebViewManagerService extends IMiuiWebViewManager.Stub {
    private static final List<String> EXEMPT_APPS = new ArrayList<String>() { // from class: com.miui.server.MiuiWebViewManagerService.1
        {
            add("com.google.android.gms");
            add("com.android.thememanager");
            add("com.google.android.setupwizard");
        }
    };
    private static final String SEND_TIME_KEY = "sendTime";
    private static final String TAG = "MiuiWebViewManagerService";
    private int MSG_RESTART_WEBVIEW = RampRateController.RateStateRecord.MODIFIER_RATE_ALL;
    private final Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public MiuiWebViewManagerService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("MiuiWebViewWorker");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.miui.server.MiuiWebViewManagerService.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == MiuiWebViewManagerService.this.MSG_RESTART_WEBVIEW) {
                    long send = msg.getData().getLong(MiuiWebViewManagerService.SEND_TIME_KEY);
                    ActivityManagerService am = ServiceManager.getService("activity");
                    List<String> pkgs = MiuiWebViewManagerService.this.collectWebViewProcesses(am);
                    int killed = 0;
                    if (pkgs != null) {
                        for (int i = 0; i < pkgs.size(); i++) {
                            String pkgName = pkgs.get(i);
                            if (pkgName != null) {
                                String[] splitInfo = pkgName.split("#");
                                String barePkgName = splitInfo[0];
                                int pid = Integer.valueOf(splitInfo[1]).intValue();
                                if (!MiuiWebViewManagerService.EXEMPT_APPS.contains(barePkgName)) {
                                    am.forceStopPackage(barePkgName, MiuiWebViewManagerService.this.mContext.getUserId());
                                    Log.d(MiuiWebViewManagerService.TAG, "kill pkgName: " + barePkgName + " pid: " + pid);
                                    killed++;
                                }
                            }
                        }
                        Log.d(MiuiWebViewManagerService.TAG, "restart webview procs: " + pkgs.size() + " with timeUsage: " + (System.currentTimeMillis() - send) + "ms killed: " + killed);
                    }
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<String> collectWebViewProcesses(ActivityManagerService am) {
        List<String> pkgsToKill = new ArrayList<>();
        try {
            ArrayList<ProcessRecord> procs = (ArrayList) ActivityManagerServiceProxy.collectProcesses.invoke(am, new Object[]{null, 0, false, new String[0]});
            if (procs == null) {
                return null;
            }
            synchronized (ActivityManagerServiceProxy.mProcLock.get(am)) {
                for (int i = procs.size() - 1; i >= 0; i--) {
                    ProcessRecord pr = procs.get(i);
                    ArraySet<String> deps = (ArraySet) ProcessRecordProxy.getPkgDeps.invoke(pr, new Object[0]);
                    ApplicationInfo info = (ApplicationInfo) ProcessRecordProxy.info.get(pr);
                    if (deps != null && info != null && info.packageName != null && (deps.contains("com.google.android.webview") || info.packageName.equals("com.android.browser"))) {
                        String pkgName = info.packageName;
                        pkgsToKill.add(pkgName + "#" + ProcessRecordProxy.getPid.invoke(pr, new Object[0]));
                    }
                }
            }
            return pkgsToKill;
        } catch (Exception e) {
            Log.w(TAG, "MiuiWebViewManagerService.collectWebViewProcesses failed", e);
            return null;
        }
    }

    public void restartWebViewProcesses() {
        Message msg = Message.obtain();
        msg.what = this.MSG_RESTART_WEBVIEW;
        Bundle data = new Bundle();
        data.putLong(SEND_TIME_KEY, System.currentTimeMillis());
        msg.setData(data);
        this.mHandler.sendMessage(msg);
        Log.i(TAG, "restartWebViewProcesses called");
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiWebViewManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiuiWebViewManagerService(context);
        }

        public void onStart() {
            publishBinderService("miuiwebview", this.mService);
        }
    }
}
