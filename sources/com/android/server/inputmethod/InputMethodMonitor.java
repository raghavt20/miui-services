package com.android.server.inputmethod;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.inputmethod.IInputMethodSessionCallback;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerServiceStub;
import com.xiaomi.abtest.d.d;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import miui.mqsas.scout.ScoutUtils;

/* loaded from: classes.dex */
public class InputMethodMonitor {
    private static final String DUMP_DIR = "/data/miuilog/stability/resleak/fdtrack/";
    static final int LEAK_LOW_WATERMARK = 100;
    private static final String TAG = "InputMethodMonitor";
    private final ArrayList<WeakReference<InputMethodInfo>> mCache;
    private int mHighWatermark;
    private final WeakHashMap<IInputMethodSessionCallback.Stub, InputMethodInfo> mRecord;
    boolean mWarning;

    /* loaded from: classes.dex */
    private static final class SINGLETON {
        private static final InputMethodMonitor INSTANCE = new InputMethodMonitor();

        private SINGLETON() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static InputMethodMonitor getInstance() {
        return SINGLETON.INSTANCE;
    }

    public synchronized void enable(int highWatermark) {
        this.mHighWatermark = Math.max(highWatermark, 200);
    }

    private InputMethodMonitor() {
        this.mHighWatermark = 0;
        this.mRecord = new WeakHashMap<>();
        this.mCache = new ArrayList<>();
    }

    public synchronized void startCreateSession(int userId, ComponentName componentName, IInputMethodSessionCallback.Stub callback) {
        if (this.mHighWatermark == 0) {
            return;
        }
        expungeStaleItems();
        InputMethodInfo info = getInputMethodInfo(userId, componentName);
        this.mRecord.put(callback, info);
        if (this.mRecord.size() == this.mHighWatermark && !this.mWarning) {
            this.mWarning = true;
            new Thread(new Runnable() { // from class: com.android.server.inputmethod.InputMethodMonitor$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InputMethodMonitor.this.scheduleAppDeath();
                }
            }, TAG).start();
        }
    }

    public synchronized void finishCreateSession(IInputMethodSessionCallback.Stub callback) {
        if (this.mHighWatermark == 0) {
            return;
        }
        expungeStaleItems();
        this.mRecord.remove(callback);
    }

    private void expungeStaleItems() {
        for (int i = this.mCache.size() - 1; i >= 0; i--) {
            if (this.mCache.get(i).get() == null) {
                this.mCache.remove(i);
            }
        }
    }

    private InputMethodInfo getInputMethodInfo(int userId, ComponentName componentName) {
        Iterator<WeakReference<InputMethodInfo>> it = this.mCache.iterator();
        while (it.hasNext()) {
            WeakReference<InputMethodInfo> ref = it.next();
            InputMethodInfo info = ref.get();
            if (info != null && info.component.equals(componentName) && info.userId == userId) {
                return info;
            }
        }
        InputMethodInfo info2 = new InputMethodInfo(componentName, userId);
        this.mCache.add(new WeakReference<>(info2));
        return info2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAppDeath() {
        boolean inTest = !ActivityThread.isSystem();
        doAppDeath(inTest);
        SystemClock.sleep(inTest ? 1L : TimeUnit.SECONDS.toMillis(5L));
        System.gc();
        System.runFinalization();
        synchronized (this) {
            int waitCount = inTest ? 0 : 10;
            while (this.mRecord.size() >= this.mHighWatermark) {
                int waitCount2 = waitCount - 1;
                if (waitCount <= 0) {
                    break;
                }
                try {
                    wait(TimeUnit.SECONDS.toMillis(5L));
                    waitCount = waitCount2;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            this.mWarning = false;
        }
    }

    private void doAppDeath(boolean inTest) {
        InputMethodInfo evil = findTheEvil();
        if (evil == null || inTest) {
            return;
        }
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        Intent intent = new Intent();
        intent.setComponent(evil.component);
        List<ResolveInfo> serviceInfos = pm.queryIntentServices(intent, 0L, 0, evil.userId);
        if (serviceInfos.size() != 1) {
            return;
        }
        String process = serviceInfos.get(0).serviceInfo.processName;
        String packageName = evil.component.getPackageName();
        if (process == null) {
            process = packageName;
        }
        if (ScoutUtils.isLibraryTest()) {
            dumpBacktrace(process);
            dumpHprof(evil, process);
        }
        Log.i(TAG, "Too many createSession requests unhandled, killing " + packageName + ", userId=" + evil.userId);
        ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        am.killProcess(process, serviceInfos.get(0).serviceInfo.applicationInfo.uid, "ime_hang");
    }

    InputMethodInfo findTheEvil() {
        ArraySet<InputMethodInfo> infos = new ArraySet<>();
        synchronized (this) {
            for (Map.Entry<IInputMethodSessionCallback.Stub, InputMethodInfo> entry : this.mRecord.entrySet()) {
                IInputMethodSessionCallback.Stub k = entry.getKey();
                InputMethodInfo v = entry.getValue();
                if (k != null && v != null) {
                    if (infos.add(v)) {
                        v.count_ = 0;
                    }
                    v.count_++;
                }
            }
        }
        InputMethodInfo evil = null;
        int maxCount = 100;
        Iterator<InputMethodInfo> it = infos.iterator();
        while (it.hasNext()) {
            InputMethodInfo app = it.next();
            Log.i(TAG, app.count_ + " createSession requests sent to " + app.component.getPackageName());
            if (app.count_ > maxCount) {
                maxCount = app.count_;
                evil = app;
            }
        }
        return evil;
    }

    private static void dumpBacktrace(String process) {
        ScoutUtils.ensureDumpDir(DUMP_DIR);
        ScoutUtils.removeHistoricalDumps(DUMP_DIR, ".trace", 2);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        String datetime = dateFormat.format(new Date());
        File traceFile = new File(DUMP_DIR, "inputmethod_hang_" + process + d.h + datetime + ".trace");
        int[] pids = Process.getPidsForCommands(new String[]{process});
        if (pids == null || pids.length == 0) {
            Log.d(TAG, "failed to dump trace of " + process + ": process disappeared");
            return;
        }
        String path = ActivityManagerServiceStub.get().dumpMiuiStackTraces(pids);
        if (path != null) {
            File file = new File(path);
            file.renameTo(traceFile);
        }
    }

    private static void dumpHprof(InputMethodInfo app, String process) {
        boolean dumping;
        File heapFile = new File(DUMP_DIR, "inputmethod_" + process + ".hprof");
        final CountDownLatch latch = new CountDownLatch(1);
        File temp = new File("/data/data/android/cache/" + heapFile.getName() + ".tmp");
        try {
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(temp, 939524096);
                dumping = ActivityManager.getService().dumpHeap(process, app.userId, true, false, false, temp.getAbsolutePath(), fd, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.inputmethod.InputMethodMonitor$$ExternalSyntheticLambda1
                    public final void onResult(Bundle bundle) {
                        latch.countDown();
                    }
                }));
            } catch (Exception e) {
                Log.w(TAG, "failed to dump hprof of " + process + ": " + e.getMessage());
            }
            if (dumping && latch.await(60L, TimeUnit.SECONDS)) {
                if (temp.length() > 0) {
                    FileUtils.copy(temp, heapFile);
                    Log.i(TAG, "hprof saved to " + heapFile);
                }
                return;
            }
            Log.w(TAG, "failed to dump hprof of " + process + ": timeout");
        } finally {
            temp.delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class InputMethodInfo {
        final ComponentName component;
        int count_;
        final int userId;

        public InputMethodInfo(ComponentName info, int userId) {
            this.component = info;
            this.userId = userId;
        }
    }
}
