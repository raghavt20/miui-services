package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FileUtils;
import android.os.Handler;
import android.os.SELinux;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.AnimationThread;
import com.android.server.IoThread;
import com.android.server.MiuiBgThread;
import com.android.server.UiThread;
import com.android.server.am.MemoryFreezeStub;
import com.android.server.am.ProcessRecord;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import miuix.appcompat.app.AlertDialog;

/* loaded from: classes.dex */
public class MiuiFreezeImpl implements MiuiFreezeStub {
    private static final String FILE_PATH = "/data/system/memgreezer/notify_splash_dismiss_switch";
    private static final String MEMFREEZE_IMAGAE_UNKNOWN = "unknown";
    private static final String PUBG_PACKAGE_NAME = "com.tencent.tmgp.pubgmhd";
    private static final String PUBG_SPLASH_CLASS_NAME = "com.epicgames.ue4.SplashActivity";
    private static final int SPLASH_Z_ORDOR_LEVEL = -1;
    private static final String TAG = "MiuiFreezeImpl";
    private Handler mAnimationHandler;
    private Handler mBgHandler;
    private int mCurPid;
    private int mCurUid;
    private volatile boolean mDialogShowing;
    private Handler mIoHandler;
    private MiuiLoadingDialog mLoadingDialog;
    private String mPackageName;
    private BroadcastReceiver mReceiver;
    private MiuiRecognizeScene mScene;
    private volatile State mState;
    private Context mUiContext;
    private Handler mUiHandler;
    private static final String DIALOG_DELAY_SHOW_TIME_PROP = "persist.sys.memfreeze.dialog.delay.time";
    private static final int DIALOG_DELAY_IMAGE_REC_TIME = SystemProperties.getInt(DIALOG_DELAY_SHOW_TIME_PROP, 1000);
    private static final String DELAY_REMOVE_SPLASH_PROP = "persist.miui.freeze.time";
    private static final int DELAY_REMOVE_SPLASH_TIME = SystemProperties.getInt(DELAY_REMOVE_SPLASH_PROP, 8000);
    private static final String MEMFREEZE_ENABLE_DEBUG = "persist.sys.memfreeze.debug";
    private static final boolean DEBUG = SystemProperties.getBoolean(MEMFREEZE_ENABLE_DEBUG, false);
    private static MiuiFreezeImpl sInstance = null;
    private MiuiFreeFormManagerService mffms = null;
    private ActivityTaskManagerService mAtms = null;
    private final ShowDialogRunnable mShowDialogRunnable = new ShowDialogRunnable();
    private DialogShowListener dialogShowListener = new DialogShowListener();
    private boolean isFreeForm = false;
    private AtomicBoolean atomicImageRecEnd = new AtomicBoolean(false);
    private Runnable mRemoveStartingRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda5
        @Override // java.lang.Runnable
        public final void run() {
            MiuiFreezeImpl.this.lambda$new$0();
        }
    };
    private Runnable mRecogImageRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda6
        @Override // java.lang.Runnable
        public final void run() {
            MiuiFreezeImpl.this.lambda$new$1();
        }
    };
    private Runnable mRecogTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda7
        @Override // java.lang.Runnable
        public final void run() {
            MiuiFreezeImpl.this.lambda$new$2();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public enum State {
        DISABLE,
        ENABLE
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreezeImpl> {

        /* compiled from: MiuiFreezeImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreezeImpl INSTANCE = new MiuiFreezeImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiFreezeImpl m2553provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiFreezeImpl m2552provideNewInstance() {
            return new MiuiFreezeImpl();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        Slog.d(TAG, "mRemoveStartingRunnable timeout");
        finishAndRemoveSplashScreen(this.mPackageName, this.mCurUid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1() {
        Slog.d(TAG, "start image recognize ");
        String result = MEMFREEZE_IMAGAE_UNKNOWN;
        try {
            result = this.mScene.recognizeScene(this.mPackageName);
            if (DEBUG) {
                Slog.d(TAG, "image recog result is " + result);
            }
        } catch (Exception e) {
            Slog.e(TAG, "image recog exception ");
        }
        processRecgResult(result);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$2() {
        processRecgResult(MEMFREEZE_IMAGAE_UNKNOWN);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public MiuiFreezeImpl() {
        Slog.d(TAG, "MiuiFreezeImpl is Initialized!");
        if (sInstance == null) {
            sInstance = this;
        }
    }

    public static MiuiFreezeImpl getImpl() {
        return sInstance;
    }

    private boolean isPropEnable() {
        return MemoryFreezeStub.getInstance().isEnable();
    }

    public void init(ActivityTaskManagerService atms) {
        if (isPropEnable()) {
            this.mAtms = atms;
            this.mAnimationHandler = new Handler(AnimationThread.getHandler().getLooper());
            this.mIoHandler = new Handler(IoThread.getHandler().getLooper());
            this.mUiHandler = new Handler(UiThread.getHandler().getLooper());
            this.mBgHandler = new Handler(MiuiBgThread.get().getLooper());
            this.mUiContext = atms.mUiContext;
            this.mScene = new MiuiRecognizeScene(this.mUiContext);
            this.mIoHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiFreezeImpl.this.lambda$init$3();
                }
            });
            this.mffms = (MiuiFreeFormManagerService) atms.mMiuiFreeFormManagerService;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$3() {
        createFile(new File(FILE_PATH));
    }

    public boolean needShowLoading(String packageName) {
        return isPropEnable() && !this.isFreeForm && MemoryFreezeStub.getInstance().isMemoryFreezeWhiteList(packageName);
    }

    public void finishAndRemoveSplashScreen(String packageName, int uid) {
        if (!needShowLoading(packageName) || this.mCurUid != uid) {
            return;
        }
        Log.d(TAG, "finishAndRemoveSplashScreen packageName: " + packageName);
        Trace.traceBegin(32L, "MF#finishAndRemoveSplashScreen");
        removeTimeRunnable(packageName, uid);
        hideDialogIfNeed(packageName);
        postRemoveSplashRunnable(packageName, uid);
        Trace.traceEnd(32L);
    }

    private void processRecgResult(String result) {
        if (!this.atomicImageRecEnd.compareAndSet(false, true)) {
            return;
        }
        if (TextUtils.equals(result, MEMFREEZE_IMAGAE_UNKNOWN) && TextUtils.equals(getTopFocusTaskPackageName(), this.mPackageName)) {
            this.mUiHandler.post(this.mShowDialogRunnable);
        } else {
            finishAndRemoveSplashScreen(this.mPackageName, this.mCurUid);
        }
    }

    private String getTopFocusTaskPackageName() {
        Task task = this.mAtms.getTopDisplayFocusedRootTask();
        return task == null ? "" : task.getPackageName();
    }

    public void checkFreeForm(String packageName, int taskId) {
        MiuiFreeFormManagerService miuiFreeFormManagerService;
        if (!isPropEnable() || !MemoryFreezeStub.getInstance().isMemoryFreezeWhiteList(packageName) || (miuiFreeFormManagerService = this.mffms) == null || miuiFreeFormManagerService.mFreeFormActivityStacks == null) {
            return;
        }
        this.isFreeForm = this.mffms.mFreeFormActivityStacks.containsKey(Integer.valueOf(taskId));
    }

    private void removeTimeRunnable(String packageName, int uid) {
        if (MiuiRecognizeScene.REC_ENABLE) {
            this.mBgHandler.removeCallbacks(this.mRecogImageRunnable);
            this.mUiHandler.removeCallbacks(this.mRecogTimeoutRunnable);
        }
        this.mUiHandler.removeCallbacks(this.mShowDialogRunnable);
        this.mAnimationHandler.removeCallbacks(this.mRemoveStartingRunnable);
    }

    private void postRemoveSplashRunnable(String packageName, int uid) {
        synchronized (this.mAtms.mGlobalLock) {
            WindowProcessController app = this.mAtms.getProcessController(packageName, uid);
            if (app != null && app.hasThread()) {
                for (ActivityRecord record : app.getActivities()) {
                    this.mAnimationHandler.post(record.mRemoveSplashRunnable);
                }
            }
        }
    }

    public void showDialogIfNeed(String packageName, ActivityRecord record, int uid) {
        if (!needShowLoading(packageName)) {
            return;
        }
        writeFile(State.ENABLE);
        registerReceiver();
        this.mAnimationHandler.removeCallbacks(this.mRemoveStartingRunnable);
        this.mAnimationHandler.postDelayed(this.mRemoveStartingRunnable, DELAY_REMOVE_SPLASH_TIME);
        if (MiuiRecognizeScene.REC_ENABLE) {
            this.mBgHandler.postDelayed(this.mRecogImageRunnable, DIALOG_DELAY_IMAGE_REC_TIME);
            this.atomicImageRecEnd.set(false);
            this.mUiHandler.postDelayed(this.mRecogTimeoutRunnable, r2 * 2);
        } else {
            this.mUiHandler.postDelayed(this.mShowDialogRunnable, DIALOG_DELAY_IMAGE_REC_TIME);
        }
        this.mPackageName = packageName;
        this.mCurUid = uid;
        synchronized (this.mAtms.mGlobalLock) {
            WindowProcessController callerApp = this.mAtms.getProcessController(packageName, uid);
            if (callerApp != null) {
                this.mCurPid = callerApp.getPid();
            }
        }
    }

    public void reportAppDied(ProcessRecord app) {
        if (app != null && TextUtils.equals(app.processName, this.mPackageName)) {
            Log.i(TAG, "reportAppDied start hide dialog when process die");
            finishAndRemoveSplashScreen(this.mPackageName, this.mCurUid);
        }
    }

    private void hideDialogIfNeed(String packageName) {
        if (!needShowLoading(packageName)) {
            return;
        }
        dismissDialog();
        writeFile(State.DISABLE);
        unRegisterReceiver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissDialog() {
        this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreezeImpl.this.lambda$dismissDialog$4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$dismissDialog$4() {
        if (this.mLoadingDialog == null) {
            return;
        }
        if (this.mDialogShowing) {
            this.mLoadingDialog.dismiss();
            this.mLoadingDialog = null;
            this.mDialogShowing = false;
            Log.d(TAG, "start dismiss dialog");
            return;
        }
        this.dialogShowListener.setNeedDismissDialog(true);
    }

    public boolean isSameProcess(int newPid) {
        boolean result = this.mCurPid == newPid;
        if (!result) {
            Log.i(TAG, "isSameProcess process has changed");
        }
        return result;
    }

    public void reparentPubgGame(ActivityRecord from, boolean isProcessExists) {
        if (from == null || !isProcessExists) {
            Log.i(TAG, "reparentPubgGame AR is null or process not exist, return");
        } else if (!needShowLoading(from.packageName) || !isPubgSplash(from)) {
            Log.i(TAG, "reparentPubgGame not need show or not from pubg splash, return");
        } else {
            from.needSkipAssignLayer = true;
        }
    }

    private boolean isPubgSplash(ActivityRecord activity) {
        if (activity == null || activity.mActivityComponent == null) {
            return false;
        }
        String packageName = activity.mActivityComponent.getPackageName();
        String className = activity.mActivityComponent.getClassName();
        return PUBG_PACKAGE_NAME.equals(packageName) && PUBG_SPLASH_CLASS_NAME.equals(className);
    }

    private void registerReceiver() {
        this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreezeImpl.this.lambda$registerReceiver$5();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerReceiver$5() {
        if (this.mReceiver != null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "start register broadcast");
        }
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.MiuiFreezeImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    Log.d(MiuiFreezeImpl.TAG, "receive broadcast,try remove");
                    MiuiFreezeImpl miuiFreezeImpl = MiuiFreezeImpl.this;
                    miuiFreezeImpl.finishAndRemoveSplashScreen(miuiFreezeImpl.mPackageName, MiuiFreezeImpl.this.mCurUid);
                }
            }
        };
        this.mReceiver = broadcastReceiver;
        this.mUiContext.registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), null, this.mUiHandler, 2);
    }

    private void unRegisterReceiver() {
        this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreezeImpl.this.lambda$unRegisterReceiver$6();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$unRegisterReceiver$6() {
        if (this.mReceiver == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "start unRegister broadcast");
        }
        try {
            this.mUiContext.unregisterReceiver(this.mReceiver);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "unregisterReceiver threw exception: " + e.getMessage());
        }
        this.mReceiver = null;
    }

    private void createFile(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                Log.e(TAG, "parent not exists");
                parent.mkdir();
                file.createNewFile();
                FileUtils.setPermissions(parent.getAbsolutePath(), 509, -1, -1);
                FileUtils.setPermissions(file.getAbsolutePath(), 509, -1, -1);
                String ctx = SELinux.fileSelabelLookup(file.getAbsolutePath());
                if (ctx == null) {
                    Slog.wtf(TAG, "Failed to get SELinux context for " + file.getAbsolutePath());
                }
                if (SELinux.setFileContext(file.getAbsolutePath(), ctx)) {
                    Slog.wtf(TAG, "Failed to set SELinux context");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "writeFile packageName: ", e);
        }
    }

    private void writeFile(final State state) {
        if (this.mState == state) {
            return;
        }
        this.mState = state;
        Log.d(TAG, "write file " + this.mState);
        this.mIoHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreezeImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreezeImpl.this.lambda$writeFile$7(state);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:15:0x002b -> B:8:0x003e). Please report as a decompilation issue!!! */
    public /* synthetic */ void lambda$writeFile$7(State state) {
        File file = new File(FILE_PATH);
        FileWriter fileWriter = null;
        try {
            try {
                try {
                    createFile(file);
                    fileWriter = new FileWriter(file);
                    fileWriter.write(state == State.ENABLE ? "1" : "0");
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    Log.e(TAG, "writeFile close: ", e);
                }
            } catch (IOException e2) {
                Log.e(TAG, "writeFile packageName: ", e2);
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        } catch (Throwable th) {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e3) {
                    Log.e(TAG, "writeFile close: ", e3);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ShowDialogRunnable implements Runnable {
        private ShowDialogRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (MiuiFreezeImpl.this.mLoadingDialog != null || MiuiFreezeImpl.this.mReceiver == null) {
                Log.i(MiuiFreezeImpl.TAG, "dialog is showing or mReceiver is null ");
                return;
            }
            MiuiFreezeImpl.this.mLoadingDialog = new MiuiLoadingDialog(MiuiFreezeImpl.this.mUiContext);
            MiuiFreezeImpl.this.mLoadingDialog.setOnShowAnimListener(MiuiFreezeImpl.this.dialogShowListener);
            MiuiFreezeImpl.this.mLoadingDialog.show();
            Log.d(MiuiFreezeImpl.TAG, "start show dialog");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DialogShowListener implements AlertDialog.OnDialogShowAnimListener {
        private boolean isNeedDismissDialog;

        private DialogShowListener() {
            this.isNeedDismissDialog = false;
        }

        void setNeedDismissDialog(boolean needDismissDialog) {
            this.isNeedDismissDialog = needDismissDialog;
        }

        public void onShowAnimComplete() {
            Log.d(MiuiFreezeImpl.TAG, "onShowAnimComplete");
            MiuiFreezeImpl.this.mDialogShowing = true;
            if (this.isNeedDismissDialog) {
                setNeedDismissDialog(false);
                MiuiFreezeImpl.this.dismissDialog();
            }
        }

        public void onShowAnimStart() {
        }
    }
}
