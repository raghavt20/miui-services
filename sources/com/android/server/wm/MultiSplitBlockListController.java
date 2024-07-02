package com.android.server.wm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.server.DisplayThread;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class MultiSplitBlockListController implements IController {
    private static final String TAG = "MultiSplitBlockListController";
    private final ActivityTaskManagerService mAtm;
    private final WindowManagerGlobalLock mGlobalLock;
    private H mH;
    private final ArrayList<String> mBlocklistPackages = new ArrayList<>();
    private final Set<String> mDeferredBlocklistPackages = new HashSet();
    private final Object mLock = new Object();
    final Consumer<ConcurrentHashMap<String, String>> mMultiSplitBlocklistChangedCallback = new Consumer() { // from class: com.android.server.wm.MultiSplitBlockListController$$ExternalSyntheticLambda0
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            MultiSplitBlockListController.this.lambda$new$0((ConcurrentHashMap) obj);
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(ConcurrentHashMap map) {
        synchronized (this.mLock) {
            this.mBlocklistPackages.clear();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.equals(FoldablePackagePolicy.POLICY_VALUE_BLOCK_LIST)) {
                    this.mBlocklistPackages.add(key);
                }
            }
            this.mH.removeMessages(1);
            this.mH.sendEmptyMessage(1);
        }
    }

    public MultiSplitBlockListController(ActivityTaskManagerService atm) {
        this.mAtm = atm;
        this.mGlobalLock = atm.mGlobalLock;
        initialize();
    }

    private ArrayList<String> getFinalBlockListLocked() {
        ArrayList<String> finalBlocklistPackages;
        synchronized (this.mLock) {
            finalBlocklistPackages = new ArrayList<>(this.mBlocklistPackages);
            for (String pkgName : this.mDeferredBlocklistPackages) {
                if (finalBlocklistPackages.contains(pkgName)) {
                    finalBlocklistPackages.remove(pkgName);
                } else {
                    finalBlocklistPackages.add(pkgName);
                }
            }
        }
        return finalBlocklistPackages;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeferredBlockListLocked() {
        this.mDeferredBlocklistPackages.clear();
        Iterator it = this.mAtm.mTaskSupervisor.mRecentTasks.getRawTasks().iterator();
        while (it.hasNext()) {
            Task task = (Task) it.next();
            if (task.realActivity != null && !this.mDeferredBlocklistPackages.contains(task.realActivity.getPackageName())) {
                this.mDeferredBlocklistPackages.add(task.realActivity.getPackageName());
            }
        }
        if (!this.mDeferredBlocklistPackages.isEmpty()) {
            Slog.d(TAG, "updateDeferredBlockListLocked: " + this.mDeferredBlocklistPackages);
        }
    }

    @Override // com.android.server.wm.IController
    public void dumpLocked(PrintWriter pw, String prefix) {
        pw.println("[MultiSplitBlockListController]");
        synchronized (this.mLock) {
            if (!this.mBlocklistPackages.isEmpty()) {
                pw.println("(mBlocklistPackages)");
                pw.print(prefix);
                int numPrint = 0;
                Iterator<String> iterator = this.mBlocklistPackages.iterator();
                while (iterator.hasNext()) {
                    pw.print(iterator.next());
                    numPrint++;
                    if (numPrint % 5 == 0) {
                        pw.println();
                        pw.print(prefix);
                    }
                    pw.print(" ");
                }
                pw.println();
            }
        }
        if (!this.mDeferredBlocklistPackages.isEmpty()) {
            pw.println("(mDeferredBlocklistPackages)");
            pw.print(prefix);
            int numPrint2 = 0;
            Iterator<String> iterator2 = this.mDeferredBlocklistPackages.iterator();
            while (iterator2.hasNext()) {
                pw.print(iterator2.next());
                numPrint2++;
                if (numPrint2 % 5 == 0) {
                    pw.println();
                    pw.print(prefix);
                }
                pw.print(" ");
            }
            pw.println();
        }
    }

    ArrayList<String> getBlocklistAppList() {
        ArrayList<String> arrayList;
        synchronized (this.mGlobalLock) {
            WindowManagerService.boostPriorityForLockedSection();
            arrayList = getFinalBlockListLocked();
            WindowManagerService.resetPriorityAfterLockedSection();
        }
        return arrayList;
    }

    ArrayList<String> getOriBlocklistAppList() {
        ArrayList<String> arrayList;
        synchronized (this.mGlobalLock) {
            arrayList = new ArrayList<>(this.mBlocklistPackages);
        }
        return arrayList;
    }

    Consumer<ConcurrentHashMap<String, String>> getMultiSplitBlocklistChangedCallbackLocked() {
        return this.mMultiSplitBlocklistChangedCallback;
    }

    @Override // com.android.server.wm.IController
    public void initialize() {
        this.mH = new H(DisplayThread.get().getLooper());
    }

    boolean isBlocklistApp(String packageName) {
        boolean isBlockApp;
        if (packageName == null) {
            return false;
        }
        synchronized (this.mGlobalLock) {
            WindowManagerService.boostPriorityForLockedSection();
            isBlockApp = getFinalBlockListLocked().contains(packageName);
            WindowManagerService.resetPriorityAfterLockedSection();
        }
        return isBlockApp;
    }

    boolean isMultiSplitBlocklistPackageLocked(String packageName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mBlocklistPackages.contains(packageName);
        }
        return contains;
    }

    void removeFromDeferredBlocklistIfNeeedLocked(Task task) {
        if (task.realActivity == null) {
            return;
        }
        String pkgName = task.realActivity.getPackageName();
        this.mDeferredBlocklistPackages.remove(pkgName);
    }

    @Override // com.android.server.wm.IController
    public void setWindowManager(WindowManagerService wm) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class H extends Handler {
        private static final int MSG_MULTI_SPLIT_BLOCK_LIST_CHANGED = 1;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (MultiSplitBlockListController.this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    MultiSplitBlockListController.this.updateDeferredBlockListLocked();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }
}
