package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.IWindow;
import com.android.internal.os.ByteTransferPipe;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class MiuiDisplayReportImpl implements MiuiDisplayReportStub {
    private static final String TAG = "ReportHelper";
    private static final ArraySet<String> WHITE_LIST_FOR_TRACING;
    private ActivityManagerInternal mAmInternal;
    private ShellCommand mShellCmd;
    private WindowManagerService mWmInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDisplayReportImpl> {

        /* compiled from: MiuiDisplayReportImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDisplayReportImpl INSTANCE = new MiuiDisplayReportImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiDisplayReportImpl m2517provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiDisplayReportImpl m2516provideNewInstance() {
            return new MiuiDisplayReportImpl();
        }
    }

    static {
        ArraySet<String> arraySet = new ArraySet<>();
        WHITE_LIST_FOR_TRACING = arraySet;
        arraySet.add("com.miui.uireporter");
    }

    public void init(ShellCommand shellcmd, WindowManagerService wms) {
        this.mShellCmd = shellcmd;
        this.mWmInternal = wms;
    }

    public boolean traceEnableForCaller() {
        if (this.mAmInternal == null) {
            this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
        if (this.mAmInternal == null) {
            return false;
        }
        int callingPid = Binder.getCallingPid();
        String packageName = this.mAmInternal.getPackageNameByPid(callingPid);
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return WHITE_LIST_FOR_TRACING.contains(packageName);
    }

    public int dumpViewCaptures() {
        WindowManagerService windowManagerService = this.mWmInternal;
        if (windowManagerService == null || this.mShellCmd == null) {
            return 0;
        }
        if (!windowManagerService.checkCallingPermission("android.permission.DUMP", "runDumpViewCapture()")) {
            throw new SecurityException("Requires DUMP permission");
        }
        final String windowName = this.mShellCmd.getNextArgRequired();
        final String viewName = this.mShellCmd.getNextArgRequired();
        if (TextUtils.isEmpty(windowName) || TextUtils.isEmpty(viewName)) {
            return 0;
        }
        final ByteTransferPipe[] pip = new ByteTransferPipe[1];
        FileOutputStream out = (FileOutputStream) this.mShellCmd.getRawOutputStream();
        synchronized (this.mWmInternal.mGlobalLock) {
            this.mWmInternal.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiDisplayReportImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    MiuiDisplayReportImpl.this.lambda$dumpViewCaptures$0(windowName, viewName, pip, (WindowState) obj);
                }
            }, false);
        }
        try {
            byte[] data = pip[0].get();
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$dumpViewCaptures$0(String windowName, String viewName, ByteTransferPipe[] pip, WindowState w) {
        if (w.isVisible() && windowName.equals(w.getName())) {
            ByteTransferPipe pipe = null;
            try {
                pipe = new ByteTransferPipe();
                ParcelFileDescriptor pfd = pipe.getWriteFd();
                if (w.isClientLocal()) {
                    dumpLocalWindowAsync(w.mClient, "CAPTURE", viewName, pfd);
                } else {
                    w.mClient.executeCommand("CAPTURE", viewName, pfd);
                }
                pip[0] = pipe;
            } catch (RemoteException | IOException e) {
                if (pipe != null) {
                    pipe.kill();
                }
            }
        }
    }

    private void dumpLocalWindowAsync(final IWindow client, final String cmd, final String params, final ParcelFileDescriptor pfd) {
        IoThread.getExecutor().execute(new Runnable() { // from class: com.android.server.wm.MiuiDisplayReportImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiDisplayReportImpl.this.lambda$dumpLocalWindowAsync$1(client, cmd, params, pfd);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$dumpLocalWindowAsync$1(IWindow client, String cmd, String params, ParcelFileDescriptor pfd) {
        synchronized (this.mWmInternal.mGlobalLock) {
            try {
                client.executeCommand(cmd, params, pfd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
