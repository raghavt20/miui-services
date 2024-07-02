package com.android.server.am;

import android.app.IApplicationThread;
import android.os.Binder;
import android.os.BinderProxy;
import android.os.BinderStub;
import android.os.SystemClock;
import android.util.LogPrinter;
import android.util.Slog;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.FastPrintWriter;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.security.AccessControlImpl;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import miui.mqsas.scout.ScoutUtils;

/* loaded from: classes.dex */
public class BinderProxyMonitorImpl extends BinderProxyMonitor {
    private static final String BINDER_ALLOC_PREFIX = "binder_alloc_u";
    private static final String DUMP_DIR = "/data/miuilog/stability/resleak/binderproxy";
    private static final String TAG = "BinderProxyMonitor";
    private volatile int mTrackedUid = -1;
    private int mLastBpCount = 0;
    private long mLastDumpTime = 0;
    private final AtomicBoolean mDumping = new AtomicBoolean(false);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BinderProxyMonitorImpl> {

        /* compiled from: BinderProxyMonitorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BinderProxyMonitorImpl INSTANCE = new BinderProxyMonitorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BinderProxyMonitorImpl m461provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BinderProxyMonitorImpl m460provideNewInstance() {
            return new BinderProxyMonitorImpl();
        }
    }

    public void trackBinderAllocations(final ActivityManagerService ams, final int targetUid) {
        if (!BinderStub.get().isEnabled()) {
            return;
        }
        ams.mHandler.post(new Runnable() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BinderProxyMonitorImpl.this.lambda$trackBinderAllocations$1(targetUid, ams);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackBinderAllocations$1(final int targetUid, ActivityManagerService ams) {
        int currBpCount = BinderProxy.getProxyCount();
        if (targetUid == this.mTrackedUid) {
            if (targetUid > 0) {
                if (currBpCount - this.mLastBpCount >= (targetUid == 1000 ? SpeedTestModeServiceImpl.ENABLE_SPTM_MIN_MEMORY : 3000)) {
                    this.mLastBpCount = currBpCount;
                    Slog.i(TAG, "Number of binder proxies sent from uid " + this.mTrackedUid + " reached " + currBpCount + ", dump automatically");
                    LogPrinter printer = new LogPrinter(4, TAG, 3);
                    try {
                        FastPrintWriter pw = new FastPrintWriter(printer);
                        try {
                            dumpBinderProxies(ams, FileDescriptor.out, pw, true, false);
                            pw.close();
                            return;
                        } finally {
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed to dump binder allocation records", e);
                        return;
                    }
                }
                return;
            }
            return;
        }
        if (targetUid != -1) {
            Slog.i(TAG, "Enable binder tracker on uid " + targetUid);
        }
        this.mLastBpCount = currBpCount;
        final int prevTrackedUid = this.mTrackedUid;
        this.mTrackedUid = targetUid;
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.lambda$trackBinderAllocations$0(prevTrackedUid, targetUid, (ProcessRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$trackBinderAllocations$0(int prevTrackedUid, int targetUid, ProcessRecord proc) {
        try {
            IApplicationThread thread = proc.getThread();
            if (thread == null) {
                return;
            }
            if (prevTrackedUid != -1 && proc.uid == prevTrackedUid) {
                Slog.i(TAG, "Disable binder tracker on process " + proc.getPid());
                proc.getThread().trackBinderAllocations(false);
            } else if (targetUid != -1) {
                try {
                    if (proc.uid == targetUid) {
                        Slog.i(TAG, "Enable binder tracker on process " + proc.getPid());
                        proc.getThread().trackBinderAllocations(true);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e);
                }
            }
        } catch (Exception e2) {
            Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e2);
        }
    }

    public void trackProcBinderAllocations(final ActivityManagerService ams, final ProcessRecord targetProc) {
        int currentTrackedUid;
        if (!BinderStub.get().isEnabled() || (currentTrackedUid = this.mTrackedUid) < 0 || targetProc.uid != currentTrackedUid) {
            return;
        }
        Slog.i(TAG, "Enable binder tracker on new process " + targetProc.getPid());
        ams.mHandler.post(new Runnable() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BinderProxyMonitorImpl.this.lambda$trackProcBinderAllocations$3(ams, targetProc);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackProcBinderAllocations$3(ActivityManagerService ams, final ProcessRecord targetProc) {
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.this.lambda$trackProcBinderAllocations$2(targetProc, (ProcessRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackProcBinderAllocations$2(ProcessRecord targetProc, ProcessRecord proc) {
        if (proc != targetProc) {
            return;
        }
        try {
            if (this.mTrackedUid != -1 && proc.uid == this.mTrackedUid) {
                proc.getThread().trackBinderAllocations(true);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e);
        }
    }

    public boolean handleDumpBinderProxies(ActivityManagerService ams, FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (!BinderStub.get().isEnabled()) {
            return false;
        }
        boolean dumpDetailToFile = false;
        boolean dumpDetail = false;
        for (int i = opti; i < args.length; i++) {
            if ("--dump-details-to-file".equals(args[opti])) {
                dumpDetailToFile = true;
            } else if ("--dump-details".equals(args[opti])) {
                dumpDetail = true;
            } else if ("--track-uid".equals(args[opti])) {
                if (opti + 1 >= args.length) {
                    throw new IllegalArgumentException("--trackUid should be followed by a uid");
                }
                int uid = Integer.parseInt(args[opti + 1]);
                trackBinderAllocations(ams, uid);
                return true;
            }
        }
        if (!dumpDetail && !dumpDetailToFile) {
            return false;
        }
        dumpBinderProxies(ams, fd, pw, dumpDetailToFile, true);
        return true;
    }

    private void dumpBinderProxies(ActivityManagerService ams, FileDescriptor fd, PrintWriter pw, boolean persistToFile, boolean force) {
        int uid = this.mTrackedUid;
        if (uid < 0) {
            pw.println("Binder allocation tracker not enabled yet");
            return;
        }
        if (this.mDumping.compareAndExchange(false, true)) {
            pw.print("dumpBinderProxies() is already ongoing...");
            return;
        }
        long currentTime = SystemClock.uptimeMillis();
        if (!force && currentTime - this.mLastDumpTime <= AccessControlImpl.LOCK_TIME_OUT) {
            pw.print("dumpBinderProxies() skipped");
            this.mDumping.set(false);
            return;
        }
        this.mLastDumpTime = currentTime;
        FileOutputStream fos = null;
        if (persistToFile && ScoutUtils.ensureDumpDir(DUMP_DIR)) {
            ScoutUtils.removeHistoricalDumps(DUMP_DIR, "", 2);
            File persistFile = new File(DUMP_DIR, BINDER_ALLOC_PREFIX + uid + d.h + currentTime);
            pw.println("Dumping binder proxies to " + persistFile);
            try {
                fos = new FileOutputStream(persistFile);
                fd = fos.getFD();
                pw = new FastPrintWriter(fos);
            } catch (Exception e) {
                pw.println("Failed to open " + persistFile + ": " + e.getMessage());
                this.mDumping.set(false);
                return;
            }
        }
        long iden = Binder.clearCallingIdentity();
        try {
            try {
                ams.dumpBinderProxies(pw, 0);
                pw.write("\n\n");
                pw.flush();
                doDumpRemoteBinders(ams, uid, fd, pw);
            } catch (Exception e2) {
                pw.println("Dump binder allocations failed" + e2);
            }
        } finally {
            Binder.restoreCallingIdentity(iden);
            IoUtils.closeQuietly(fos);
            this.mDumping.set(false);
        }
    }

    private void doDumpRemoteBinders(ActivityManagerService ams, final int targetUid, final FileDescriptor fd, final PrintWriter pw) {
        if (fd == null) {
            return;
        }
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.lambda$doDumpRemoteBinders$4(targetUid, fd, pw, (ProcessRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$doDumpRemoteBinders$4(int targetUid, FileDescriptor fd, PrintWriter pw, ProcessRecord proc) {
        try {
            IApplicationThread thread = proc.getThread();
            if (thread == null || proc.uid != targetUid) {
                return;
            }
            TransferPipe tp = new TransferPipe();
            try {
                proc.getThread().dumpBinderAllocations(tp.getWriteFd());
                tp.go(fd, 10000L);
                tp.kill();
            } catch (Throwable th) {
                tp.kill();
                throw th;
            }
        } catch (Exception e) {
            if (pw != null) {
                pw.println("Failure while dumping binder traces from " + proc + ".  Exception: " + e);
                pw.flush();
            }
        }
    }
}
