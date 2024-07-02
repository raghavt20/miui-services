package com.android.server.wm;

import android.app.ResultInfo;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.DestroyActivityItem;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayCutout;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import com.google.android.collect.Sets;
import com.miui.server.AccessController;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.security.ISecurityManager;

/* loaded from: classes.dex */
public class MiuiFreeFormGestureController {
    public static boolean DEBUG = false;
    public static final String GB_BOOSTING = "gb_boosting";
    private static final String MIUI_OPTIMIZATION = "miui_optimization";
    private static final String TAG = "MiuiFreeFormGestureController";
    public static final String VTB_BOOSTING = "vtb_boosting";
    AudioManager mAudioManager;
    DisplayContent mDisplayContent;
    Handler mHandler;
    boolean mIsPortrait;
    int mLastOrientation;
    MiuiFreeFormKeyCombinationHelper mMiuiFreeFormKeyCombinationHelper;
    MiuiFreeFormManagerService mMiuiFreeFormManagerService;
    MiuiMultiWindowRecommendHelper mMiuiMultiWindowRecommendHelper;
    ActivityTaskManagerService mService;
    MiuiFreeformTrackManager mTrackManager;
    private FreeFormReceiver mFreeFormReceiver = new FreeFormReceiver();
    boolean mIsVideoMode = false;
    boolean mIsGameMode = false;
    private ContentObserver mMiuiOptObserver = new ContentObserver(new Handler((Handler.Callback) null, false)) { // from class: com.android.server.wm.MiuiFreeFormGestureController.2
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            MiuiFreeFormGestureController.this.updateCtsMode();
        }
    };
    private ContentObserver mVideoModeObserver = new ContentObserver(new Handler((Handler.Callback) null, false)) { // from class: com.android.server.wm.MiuiFreeFormGestureController.3
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
            miuiFreeFormGestureController.mIsVideoMode = Settings.Secure.getIntForUser(miuiFreeFormGestureController.mService.mContext.getContentResolver(), MiuiFreeFormGestureController.VTB_BOOSTING, 0, 0) != 0;
        }
    };
    private ContentObserver mGameModeObserver = new ContentObserver(new Handler((Handler.Callback) null, false)) { // from class: com.android.server.wm.MiuiFreeFormGestureController.4
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
            miuiFreeFormGestureController.mIsGameMode = Settings.Secure.getIntForUser(miuiFreeFormGestureController.mService.mContext.getContentResolver(), MiuiFreeFormGestureController.GB_BOOSTING, 0, 0) != 0;
        }
    };

    public MiuiFreeFormGestureController(ActivityTaskManagerService service, MiuiFreeFormManagerService miuiFreeFormManagerService, Handler handler) {
        this.mService = service;
        this.mHandler = handler;
        this.mMiuiFreeFormManagerService = miuiFreeFormManagerService;
    }

    public void onATMSSystemReady() {
        this.mDisplayContent = this.mService.mRootWindowContainer.getDisplayContent(0);
        this.mMiuiMultiWindowRecommendHelper = new MiuiMultiWindowRecommendHelper(this.mService.mContext, this.mMiuiFreeFormManagerService);
        Context context = this.mService.mContext;
        Context context2 = this.mService.mContext;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        registerFreeformCloudDataObserver();
        Context context3 = this.mService.mContext;
        FreeFormReceiver freeFormReceiver = this.mFreeFormReceiver;
        context3.registerReceiver(freeFormReceiver, freeFormReceiver.mFilter, null, this.mHandler);
        this.mService.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_optimization"), false, this.mMiuiOptObserver, -1);
        this.mService.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(VTB_BOOSTING), false, this.mVideoModeObserver, 0);
        this.mService.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(GB_BOOSTING), false, this.mGameModeObserver, 0);
        this.mMiuiFreeFormManagerService.updateDataFromSetting();
        updateCtsMode();
        this.mMiuiFreeFormKeyCombinationHelper = new MiuiFreeFormKeyCombinationHelper(this);
        this.mIsPortrait = this.mService.mContext.getResources().getConfiguration().orientation == 1;
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController.1
            @Override // java.lang.Runnable
            public void run() {
                MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
                miuiFreeFormGestureController.mTrackManager = new MiuiFreeformTrackManager(miuiFreeFormGestureController.mService.mContext, MiuiFreeFormGestureController.this);
            }
        }, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FreeFormReceiver extends BroadcastReceiver {
        IntentFilter mFilter;

        public FreeFormReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            this.mFilter = intentFilter;
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            this.mFilter.addAction("android.intent.action.USER_PRESENT");
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.updateDataFromSetting();
                    Slog.d(MiuiFreeFormGestureController.TAG, "update data from Setting for user switch new userId: " + intent.getIntExtra("android.intent.extra.user_handle", 0));
                }
                if ("android.intent.action.USER_PRESENT".equals(action)) {
                    if (MiuiFreeFormGestureController.this.mTrackManager != null) {
                        MiuiFreeFormGestureController.this.mTrackManager.bindOneTrackService();
                    } else {
                        MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
                        miuiFreeFormGestureController.mTrackManager = new MiuiFreeformTrackManager(miuiFreeFormGestureController.mService.mContext, MiuiFreeFormGestureController.this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:4:0x0034, code lost:
    
        if (android.provider.Settings.Global.getInt(r5.mService.mContext.getContentResolver(), "enable_freeform_support", 0) != 0) goto L6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void updateCtsMode() {
        /*
            r5 = this;
            java.lang.String r0 = "persist.sys.miui_optimization"
            java.lang.String r1 = "1"
            java.lang.String r2 = "ro.miui.cts"
            java.lang.String r2 = android.os.SystemProperties.get(r2)
            boolean r1 = r1.equals(r2)
            r2 = 1
            r1 = r1 ^ r2
            boolean r0 = android.os.SystemProperties.getBoolean(r0, r1)
            r0 = r0 ^ r2
            com.android.server.wm.ActivityTaskManagerService r1 = r5.mService
            android.content.Context r1 = r1.mContext
            android.content.pm.PackageManager r1 = r1.getPackageManager()
            java.lang.String r3 = "android.software.freeform_window_management"
            boolean r1 = r1.hasSystemFeature(r3)
            if (r1 != 0) goto L36
            com.android.server.wm.ActivityTaskManagerService r1 = r5.mService
            android.content.Context r1 = r1.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r3 = "enable_freeform_support"
            r4 = 0
            int r1 = android.provider.Settings.Global.getInt(r1, r3, r4)
            if (r1 == 0) goto L37
        L36:
            r4 = r2
        L37:
            r1 = r4
            com.android.server.wm.ActivityTaskManagerService r3 = r5.mService
            com.android.server.wm.WindowManagerGlobalLock r3 = r3.mGlobalLock
            monitor-enter(r3)
            if (r0 == 0) goto L44
            com.android.server.wm.ActivityTaskManagerService r2 = r5.mService     // Catch: java.lang.Throwable -> L7a
            r2.mSupportsFreeformWindowManagement = r1     // Catch: java.lang.Throwable -> L7a
            goto L48
        L44:
            com.android.server.wm.ActivityTaskManagerService r4 = r5.mService     // Catch: java.lang.Throwable -> L7a
            r4.mSupportsFreeformWindowManagement = r2     // Catch: java.lang.Throwable -> L7a
        L48:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L7a
            java.lang.String r2 = "MiuiFreeFormGestureController"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "isCtsMode: "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r4 = "freeformWindowManagement ： "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.String r4 = " mService.mSupportsFreeformWindowManagement: "
            java.lang.StringBuilder r3 = r3.append(r4)
            com.android.server.wm.ActivityTaskManagerService r4 = r5.mService
            boolean r4 = r4.mSupportsFreeformWindowManagement
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Slog.d(r2, r3)
            return
        L7a:
            r2 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L7a
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormGestureController.updateCtsMode():void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInVideoOrGameScene() {
        return this.mIsVideoMode || this.mIsGameMode;
    }

    public void registerFreeformCloudDataObserver() {
        ContentObserver contentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.wm.MiuiFreeFormGestureController.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.d(MiuiFreeFormGestureController.TAG, "receive notification of data update from app");
                MiuiMultiWindowUtils.updateListFromCloud(MiuiFreeFormGestureController.this.mService.mContext);
            }
        };
        this.mService.mContext.getContentResolver().registerContentObserver(Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify"), false, contentObserver);
    }

    public void reflectRemoveUnreachableFreeformTask() {
        try {
            Class clazz = this.mService.mTaskSupervisor.mRecentTasks.getClass();
            Method method = clazz.getDeclaredMethod("removeUnreachableFreeformTask", Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(this.mService.mTaskSupervisor.mRecentTasks, true);
        } catch (Exception e) {
            Slog.d(TAG, "getDeclaredMethod:reflectRemoveUnreachableFreeformTask" + e.toString());
        }
    }

    public void startExitApplication(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "startExitApplication");
        hideStack(mffas);
        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
        synchronized (this.mService.mGlobalLock) {
            this.mService.mWindowManager.getDefaultDisplayContentLocked().getInputMonitor().updateInputWindowsImmediately(inputTransaction);
        }
        inputTransaction.apply();
        exitFreeForm(mffas);
    }

    private void hideStack(MiuiFreeFormActivityStack stack) {
        synchronized (this.mService.mGlobalLock) {
            if (stack != null) {
                if (stack.mTask != null) {
                    hide(stack.mTask);
                }
            }
        }
    }

    private void hide(WindowContainer wc) {
        SurfaceControl sc;
        if (wc != null && (sc = wc.mSurfaceControl) != null && sc.isValid()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setCrop(sc, null);
            t.hide(sc);
            t.apply();
        }
    }

    public void clearAllFreeFormForProcessReboot() {
        Slog.d(TAG, "clearAllFreeForm for process dead");
        synchronized (this.mService.mGlobalLock) {
            for (MiuiFreeFormActivityStack mffas : this.mMiuiFreeFormManagerService.mFreeFormActivityStacks.values()) {
                if (mffas != null && mffas.mTask != null) {
                    if (mffas.mTask.getBaseIntent() != null) {
                        mffas.mTask.getBaseIntent().setMiuiFlags(mffas.mTask.getBaseIntent().getMiuiFlags() & (-257));
                    }
                    Slog.d(TAG, "exit freeform stack:" + mffas.mTask);
                    moveFreeformToFullScreen(mffas);
                    clearFreeFormSurface(mffas);
                }
            }
        }
    }

    private void clearFreeFormSurface(MiuiFreeFormActivityStack stack) {
        SurfaceControl leash;
        if (stack != null && stack.mTask != null && (leash = stack.mTask.getSurfaceControl()) != null && leash.isValid()) {
            Slog.d(TAG, "clear freeform surface: taskId=" + stack.mTask.mTaskId);
            SurfaceControl.Transaction tx = new SurfaceControl.Transaction();
            float f = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            tx.setPosition(leash, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            tx.setScale(leash, 1.0f, 1.0f);
            tx.setAlpha(leash, 1.0f);
            tx.setWindowCrop(leash, 0, 0);
            tx.setCornerRadius(leash, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            tx.setSurfaceStroke(leash, new float[]{0.098f, 0.098f, 0.098f}, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            float cornerX = MiuiMultiWindowUtils.applyDip2Px(-12.0f);
            float[] tipsColors = {0.074f, 0.074f, 0.074f};
            float radius = MiuiMultiWindowUtils.applyDip2Px(18.0f);
            float thickness = MiuiMultiWindowUtils.applyDip2Px(4.0f);
            tx.setLeftBottomCornerTip(leash, cornerX, tipsColors, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, thickness, radius, 54.0f);
            tx.setRightBottomCornerTip(leash, cornerX, tipsColors, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, thickness, radius, 54.0f);
            if (MiuiMultiWindowUtils.isSupportMiuiShadowV2()) {
                Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, RectF.class};
                Object[] values = new Object[8];
                values[0] = leash;
                values[1] = 0;
                values[2] = Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                values[3] = Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                values[4] = Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                values[5] = Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                if (stack.isInFreeFormMode() || stack.inNormalPinMode()) {
                    f = MiuiMultiWindowUtils.FREEFORM_ROUND_CORNER_DIP_MIUI15;
                } else if (stack.isInMiniFreeFormMode() || stack.inMiniPinMode()) {
                    f = MiuiMultiWindowUtils.MINI_FREEFORM_ROUND_CORNER_DIP_MIUI15;
                }
                values[6] = Float.valueOf(f);
                values[7] = new RectF();
                MiuiMultiWindowUtils.callObjectMethod(tx, SurfaceControl.Transaction.class, "setMiShadow", parameterTypes, values);
            } else if (MiuiMultiWindowUtils.isSupportMiuiShadowV1()) {
                Class<?>[] parameterTypes2 = {SurfaceControl.class, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
                MiuiMultiWindowUtils.callObjectMethod(tx, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes2, new Object[]{leash, Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X), MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0, 0, 0, 1});
            }
            tx.apply();
        }
    }

    private void exitFreeForm(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "exitFreeForm");
        synchronized (this.mService.mGlobalLock) {
            if (mffas != null) {
                if (mffas.mTask != null) {
                    if (mffas.mTask.getBaseIntent() != null) {
                        mffas.mTask.getBaseIntent().setMiuiFlags(mffas.mTask.getBaseIntent().getMiuiFlags() & (-257));
                    }
                    Slog.d(TAG, "exitFreeForm freeform stack :" + mffas.mTask);
                    moveFreeformToFullScreen(mffas);
                    this.mMiuiFreeFormManagerService.onExitFreeform(mffas.mTask.mTaskId);
                }
            }
        }
    }

    public void deliverResultForResumeActivityInFreeform(ActivityRecord resultTo) {
        if (resultTo != null && resultTo.inFreeformWindowingMode() && resultTo.app != null && "com.xiaomi.payment/com.mipay.common.ui.TranslucentActivity".equals(resultTo.shortComponentName)) {
            try {
                resultTo.addResultLocked((ActivityRecord) null, (String) null, resultTo.requestCode, 0, (Intent) null);
                ClientTransaction transaction = ClientTransaction.obtain(resultTo.app.getThread(), resultTo.token);
                Slog.d(TAG, " deliverResultForResumeActivityInFreeform: delivering results to PAYMENT_TRANSLUCENTACTIVITY: " + resultTo);
                ArrayList<ResultInfo> resultInfo = new ArrayList<>();
                Intent data = new Intent();
                data.putExtra("deliverResultsReason", "ExitFreeform");
                resultInfo.add(new ResultInfo((String) null, resultTo.requestCode, 0, data));
                transaction.addCallback(ActivityResultItem.obtain(resultInfo));
                this.mService.getLifecycleManager().scheduleTransaction(transaction);
                resultTo.results = null;
            } catch (RemoteException e) {
                Slog.e(TAG, " deliverResultForResumeActivityInFreeform: fail to PAYMENT_TRANSLUCENTACTIVITY " + e);
            }
        }
    }

    public void deliverResultForExitFreeform(final MiuiFreeFormActivityStack mffas) {
        if (!isInVideoOrGameScene() || mffas == null) {
            return;
        }
        ActivityRecord activityRecord = this.mService.mTaskSupervisor.mRootWindowContainer.getDisplayContent(0).topRunningActivityExcludeFreeform();
        final Task currentFullRootTask = activityRecord == null ? null : activityRecord.getTask();
        if (currentFullRootTask == null || currentFullRootTask.getRootTaskId() != mffas.getFreeFormLaunchFromTaskId()) {
            return;
        }
        final String DELIVER_RESULT_REASON = "deliverResultsReason";
        final String EXIT_FREEFORM = "ExitFreeform";
        if (mffas.mTask != null && mffas.mTask.getTopNonFinishingActivity() != null && AccessController.APP_LOCK_CLASSNAME.equals(mffas.mTask.getTopNonFinishingActivity().shortComponentName)) {
            ActivityRecord resultFrom = mffas.mTask.getTopNonFinishingActivity();
            if (resultFrom != null && activityRecord != null && activityRecord.app != null) {
                try {
                    activityRecord.addResultLocked(resultFrom, resultFrom.resultWho, activityRecord.requestCode, 0, (Intent) null);
                    ClientTransaction transaction = ClientTransaction.obtain(activityRecord.app.getThread(), activityRecord.token);
                    Slog.d(TAG, " deliverResultForExitFreeform for applicationlock" + resultFrom + " delivering results to " + activityRecord + "results: " + activityRecord.results);
                    transaction.addCallback(ActivityResultItem.obtain(activityRecord.results));
                    this.mService.getLifecycleManager().scheduleTransaction(transaction);
                    activityRecord.results = null;
                    return;
                } catch (RemoteException e) {
                    Slog.e(TAG, " deliverResultForExitFreeform: fail " + e);
                    return;
                }
            }
            return;
        }
        mffas.mTask.forAllActivities(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiFreeFormGestureController.this.lambda$deliverResultForExitFreeform$0(currentFullRootTask, mffas, DELIVER_RESULT_REASON, EXIT_FREEFORM, (ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$deliverResultForExitFreeform$0(Task currentFullRootTask, MiuiFreeFormActivityStack mffas, String DELIVER_RESULT_REASON, String EXIT_FREEFORM, ActivityRecord r) {
        ActivityRecord resultTo;
        if (r != null && r.resultTo != null && r.resultTo.app != null) {
            ActivityRecord resultTo2 = r.resultTo;
            try {
                resultTo2.addResultLocked(r, r.resultWho, resultTo2.requestCode, 0, (Intent) null);
                ClientTransaction transaction = ClientTransaction.obtain(resultTo2.app.getThread(), resultTo2.token);
                Slog.d(TAG, " deliverResultForExitFreeform: " + r + " delivering results to " + resultTo2 + "results: " + resultTo2.results);
                transaction.addCallback(ActivityResultItem.obtain(resultTo2.results));
                this.mService.getLifecycleManager().scheduleTransaction(transaction);
                resultTo2.results = null;
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, " deliverResultForExitFreeform: fail " + e);
                return;
            }
        }
        if (!isInVideoOrGameScene() || r == null || currentFullRootTask == null || (resultTo = currentFullRootTask.getTopNonFinishingActivity()) == null || resultTo.app == null) {
            return;
        }
        try {
            resultTo.addResultLocked(r, r.resultWho, resultTo.requestCode, 0, (Intent) null);
            ClientTransaction transaction2 = ClientTransaction.obtain(resultTo.app.getThread(), resultTo.token);
            Slog.d(TAG, " deliverResultForExitFreeform: delivering results to fullscreen activity: " + resultTo);
            ArrayList<ResultInfo> resultInfo = new ArrayList<>();
            Intent data = new Intent();
            if (!"com.babycloud.hanju/com.tencent.connect.common.AssistActivity".equals(resultTo.shortComponentName) || !"com.tencent.mobileqq".equals(mffas.getStackPackageName())) {
                data.putExtra(DELIVER_RESULT_REASON, EXIT_FREEFORM);
            }
            resultInfo.add(new ResultInfo(r.resultWho, resultTo.requestCode, 0, data));
            transaction2.addCallback(ActivityResultItem.obtain(resultInfo));
            this.mService.getLifecycleManager().scheduleTransaction(transaction2);
            resultTo.results = null;
        } catch (RemoteException e2) {
            Slog.e(TAG, " deliverResultForExitFreeform: fail to fullscreen activity " + e2);
        }
    }

    public boolean ignoreDeliverResultForFreeForm(ActivityRecord resultTo, ActivityRecord resultFrom) {
        if (resultTo == null || resultFrom == null || !resultFrom.inFreeformWindowingMode() || !isInVideoOrGameScene() || !"com.tencent.mobileqq/.activity.JumpActivity".equals(resultFrom.shortComponentName)) {
            return false;
        }
        return true;
    }

    public void deliverResultForFinishActivity(ActivityRecord resultTo, final ActivityRecord resultFrom, Intent intent) {
        boolean deliverResult;
        ActivityRecord resultTo2;
        String rawPackageName;
        if (resultFrom == null || !isInVideoOrGameScene() || !resultFrom.inFreeformWindowingMode()) {
            return;
        }
        boolean deliverResult2 = false;
        if (resultTo != null) {
            if ("com.tencent.mobileqq/.activity.JumpActivity".equals(resultTo.shortComponentName) && "com.tencent.mobileqq/cooperation.qzone.share.QZoneShareActivity".equals(resultFrom.shortComponentName)) {
                ActivityRecord resultTo3 = this.mDisplayContent.topRunningActivityExcludeFreeform();
                if (resultTo3 != null && resultTo3.getTask() != null && resultTo3.app != null) {
                    resultTo2 = resultTo3;
                    deliverResult = true;
                } else {
                    return;
                }
            } else {
                resultTo2 = resultTo;
                deliverResult = false;
            }
        } else {
            ActivityRecord launchActivity = this.mDisplayContent.getActivity(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormGestureController.lambda$deliverResultForFinishActivity$1(resultFrom, (ActivityRecord) obj);
                }
            });
            ActivityRecord resultTo4 = this.mDisplayContent.topRunningActivityExcludeFreeform();
            MiuiFreeFormActivityStack mffas = this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStackForMiuiFB(resultFrom.getRootTaskId());
            if (resultTo4 == null || resultTo4.getTask() == null || resultTo4.app == null || mffas == null || resultTo4.getRootTaskId() != mffas.getFreeFormLaunchFromTaskId()) {
                return;
            }
            if ((launchActivity == null && ("com.tencent.mobileqq/.activity.JumpActivity".equals(resultFrom.shortComponentName) || "com.tencent.mobileqq/cooperation.qzone.share.QZoneShareActivity".equals(resultFrom.shortComponentName))) || (launchActivity != null && resultTo4.getTask() == launchActivity.getTask())) {
                deliverResult2 = true;
            }
            if ("com.sina.weibo/.composerinde.ComposerDispatchActivity".equals(resultFrom.shortComponentName) && "com.youku.phone/com.sina.weibo.sdk.share.ShareTransActivity".equals(resultTo4.shortComponentName)) {
                deliverResult2 = true;
            }
            if (intent != null && AccessController.APP_LOCK_CLASSNAME.equals(resultFrom.shortComponentName)) {
                Intent rawIntent = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
                if (rawIntent != null && rawIntent.getComponent() != null) {
                    String rawPackageName2 = rawIntent.getComponent().getPackageName();
                    rawPackageName = rawPackageName2;
                } else {
                    rawPackageName = null;
                }
                IBinder b = ServiceManager.getService("security");
                if (b != null) {
                    try {
                        int userId = UserHandle.getUserId(resultFrom.getUid());
                        ISecurityManager service = ISecurityManager.Stub.asInterface(b);
                        deliverResult = !service.checkAccessControlPassAsUser(rawPackageName, (Intent) null, userId);
                        resultTo2 = resultTo4;
                    } catch (Exception e) {
                        Slog.e(TAG, "deliverResultForFinishActivity checkAccessControlPass error: ", e);
                    }
                }
            }
            deliverResult = deliverResult2;
            resultTo2 = resultTo4;
        }
        if (deliverResult) {
            try {
                Intent data = new Intent();
                data.putExtra("deliverResultsReason", "ExitFreeform");
                resultTo2.addResultLocked(resultFrom, resultFrom.resultWho, resultTo2.requestCode, 0, (Intent) null);
                ClientTransaction transaction = ClientTransaction.obtain(resultTo2.app.getThread(), resultTo2.token);
                Slog.d(TAG, "deliverResultForFinishActivity: delivering results to " + resultTo2 + " resultFrom= " + resultFrom);
                ArrayList<ResultInfo> resultInfo = new ArrayList<>();
                resultInfo.add(new ResultInfo(resultFrom.resultWho, resultTo2.requestCode, 0, data));
                transaction.addCallback(ActivityResultItem.obtain(resultInfo));
                this.mService.getLifecycleManager().scheduleTransaction(transaction);
                resultTo2.results = null;
            } catch (RemoteException e2) {
                Slog.e(TAG, " deliverResultForFinishActivity: fail deliver results to " + resultTo2 + e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$deliverResultForFinishActivity$1(ActivityRecord resultFrom, ActivityRecord r) {
        return !r.finishing && r.getLaunchedFromPid() == resultFrom.getPid();
    }

    public void deliverDestroyItemToAlipay(MiuiFreeFormActivityStack mffas) {
        if (mffas.mTask == null) {
            return;
        }
        try {
            ActivityRecord topActivity = mffas.mTask.topRunningActivity();
            ActivityRecord bottomActivity = this.mService.mTaskSupervisor.mRootWindowContainer.getDisplayContent(0).getActivityBelow(topActivity);
            if (topActivity != null && bottomActivity != null && bottomActivity.app != null && "com.eg.android.AlipayGphone/com.alipay.mobile.quinox.SchemeLauncherActivity".equals(bottomActivity.shortComponentName)) {
                if ("com.eg.android.AlipayGphone/com.alipay.mobile.nebulax.integration.mpaas.activity.NebulaActivity$Main".equals(topActivity.shortComponentName) || "com.eg.android.AlipayGphone/com.alipay.mobile.nebulax.xriver.activity.XRiverActivity".equals(topActivity.shortComponentName)) {
                    Slog.d(TAG, " deliverDestroyItemToAlipay: delivering destroyitem to bottomactivity: " + bottomActivity);
                    bottomActivity.addResultLocked(topActivity, topActivity.resultWho, bottomActivity.requestCode, 0, (Intent) null);
                    ClientTransaction transaction = ClientTransaction.obtain(bottomActivity.app.getThread(), bottomActivity.token);
                    transaction.addCallback(DestroyActivityItem.obtain(false, 0));
                    this.mService.getLifecycleManager().scheduleTransaction(transaction);
                    bottomActivity.results = null;
                    bottomActivity.getTask().removeImmediately();
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, " deliverDestroyItemToAlipay: fail " + e);
        }
    }

    private void moveFreeformToFullScreen(MiuiFreeFormActivityStack mffas) {
        Task rootTask = mffas.mTask;
        if (rootTask == null || rootTask.getDisplayArea() == null) {
            return;
        }
        if (MiuiDesktopModeUtils.isDesktopActive() && mffas.inPinMode()) {
            Slog.i(TAG, "moveFreeformToFullScreen::::::remove pin task =" + rootTask);
            this.mService.mTaskSupervisor.removeRootTask(rootTask);
            return;
        }
        if (MiuiDesktopModeUtils.isDesktopActive()) {
            mffas.setIsFrontFreeFormStackInfo(false);
            this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(3, mffas);
        }
        rootTask.setForceHidden(1, true);
        rootTask.ensureActivitiesVisible((ActivityRecord) null, 0, true);
        this.mService.mTaskSupervisor.activityIdleInternal((ActivityRecord) null, false, true, (Configuration) null);
        this.mService.deferWindowLayout();
        try {
            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{rootTask});
            this.mService.mWindowManager.mTaskSnapshotController.snapshotTasks(tasks);
            this.mService.mWindowManager.mTaskSnapshotController.addSkipClosingAppSnapshotTasks(tasks);
            Configuration c = new Configuration(rootTask.getRequestedOverrideConfiguration());
            c.windowConfiguration.setAlwaysOnTop(false);
            c.windowConfiguration.setWindowingMode(0);
            c.windowConfiguration.setBounds((Rect) null);
            rootTask.onRequestedOverrideConfigurationChanged(c);
            TaskDisplayArea taskDisplayArea = rootTask.getDisplayArea();
            taskDisplayArea.positionTaskBehindHome(rootTask);
            rootTask.setForceHidden(1, false);
            this.mService.mTaskSupervisor.mRootWindowContainer.ensureActivitiesVisible((ActivityRecord) null, 0, true);
            this.mService.mTaskSupervisor.mRootWindowContainer.resumeFocusedTasksTopActivities();
        } catch (Exception e) {
        } catch (Throwable th) {
            this.mService.continueWindowLayout();
            throw th;
        }
        this.mService.continueWindowLayout();
    }

    public static int getStatusBarHeight(InsetsStateController insetsStateController) {
        return getStatusBarHeight(insetsStateController, true);
    }

    public static int getStatusBarHeight(InsetsStateController insetsStateController, boolean ignoreVisibility) {
        Rect frame;
        if (insetsStateController == null) {
            return 0;
        }
        int statusBarHeight = 0;
        SparseArray<InsetsSourceProvider> providers = insetsStateController.getSourceProviders();
        for (int i = providers.size() - 1; i >= 0; i--) {
            InsetsSourceProvider provider = providers.valueAt(i);
            if (provider.getSource().getType() != WindowInsets.Type.statusBars() && ((ignoreVisibility || provider.getSource().isVisible()) && (frame = provider.getSource().getFrame()) != null && !frame.isEmpty())) {
                statusBarHeight = Math.min(frame.height(), frame.width());
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "getStatusBarHeight statusBarHeight=" + statusBarHeight);
        }
        return statusBarHeight;
    }

    public static int getNavBarHeight(InsetsStateController insetsStateController) {
        return getNavBarHeight(insetsStateController, true);
    }

    public static int getNavBarHeight(InsetsStateController insetsStateController, boolean ignoreVisibility) {
        Rect frame;
        if (insetsStateController == null) {
            return 0;
        }
        int navBarHeight = 0;
        SparseArray<InsetsSourceProvider> providers = insetsStateController.getSourceProviders();
        for (int i = providers.size() - 1; i >= 0; i--) {
            InsetsSourceProvider provider = providers.valueAt(i);
            if (provider.getSource().getType() != WindowInsets.Type.navigationBars() && ((ignoreVisibility || provider.getSource().isVisible()) && (frame = provider.getSource().getFrame()) != null && !frame.isEmpty())) {
                navBarHeight = Math.min(frame.height(), frame.width());
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "getNavBarHeight navBarHeight=" + navBarHeight);
        }
        return navBarHeight;
    }

    public static int getDisplayCutoutHeight(DisplayFrames displayFrames) {
        if (displayFrames == null) {
            return 0;
        }
        int displayCutoutHeight = 0;
        DisplayCutout cutout = displayFrames.mInsetsState.getDisplayCutout();
        if (displayFrames.mRotation == 0) {
            displayCutoutHeight = cutout.getSafeInsetTop();
        } else if (displayFrames.mRotation == 2) {
            displayCutoutHeight = cutout.getSafeInsetBottom();
        } else if (displayFrames.mRotation == 1) {
            displayCutoutHeight = cutout.getSafeInsetLeft();
        } else if (displayFrames.mRotation == 3) {
            displayCutoutHeight = cutout.getSafeInsetRight();
        }
        if (DEBUG) {
            Slog.d(TAG, "getDisplayCutoutHeight displayCutoutHeight=" + displayCutoutHeight);
        }
        return displayCutoutHeight;
    }

    public void displayConfigurationChange(DisplayContent displayContent, Configuration configuration) {
        boolean imeVisible = false;
        this.mIsPortrait = configuration.orientation == 1;
        if (this.mLastOrientation != configuration.orientation) {
            this.mLastOrientation = configuration.orientation;
            WindowState imeWin = displayContent.mInputMethodWindow;
            if (imeWin != null && imeWin.isVisible() && imeWin.isDisplayed()) {
                imeVisible = true;
            }
            if (imeVisible) {
                Slog.d(TAG, "notify Ime showStatus for new orientation:" + configuration.orientation);
                int imeHeight = displayContent.getInputMethodWindowVisibleHeight();
                this.mMiuiFreeFormManagerService.setAdjustedForIme(true, imeHeight, true);
            }
        }
    }

    public void onFirstWindowDrawn(ActivityRecord activityRecord) {
        this.mMiuiMultiWindowRecommendHelper.onFirstWindowDrawn(activityRecord);
    }

    public boolean needForegroundPin(MiuiFreeFormActivityStack mffas) {
        String packageName = mffas.getStackPackageName();
        ActivityRecord r = mffas.mTask.getTopNonFinishingActivity();
        int uid = -1;
        if (r != null && r.info != null && r.info.applicationInfo != null) {
            uid = r.info.applicationInfo.uid;
        }
        return MiuiMultiWindowUtils.supportForeGroundPin() && !MiuiMultiWindowAdapter.isInForegroundPinAppBlackList(packageName) && ((isPlaybackActive(packageName, uid) && MiuiMultiWindowAdapter.isInAudioForegroundPinAppList(packageName)) || MiuiMultiWindowAdapter.isInForegroundPinAppWhiteList(packageName) || MiuiMultiWindowAdapter.isInTopGameList(packageName));
    }

    private boolean isPlaybackActive(String curPkg, int curUid) {
        String activeTrackString = this.mAudioManager.getParameters("audio_active_track");
        String[] activeTrackArray = activeTrackString.replace(';', ',').split(",");
        Slog.d(TAG, "isPlaybackActive: activeTrackArray[PID/UID]:" + Arrays.toString(activeTrackArray) + " curPkg:" + curPkg + " curUid:" + curUid);
        PackageManager pm = this.mService.mContext.getPackageManager();
        for (String str : activeTrackArray) {
            if (str.contains("/")) {
                String[] split = str.split("/");
                try {
                    int uid = Integer.parseInt(split[1]);
                    String[] packageNames = pm.getPackagesForUid(uid);
                    if (curUid == uid) {
                        return true;
                    }
                    if (curUid == -1 && curPkg.equals(packageNames[0])) {
                        return true;
                    }
                } catch (Exception e) {
                }
            }
        }
        Slog.d(TAG, "isPlaybackActive:false curPkg:" + curPkg + " curUid:" + curUid);
        return false;
    }

    public void freeFormAndFullScreenToggleByKeyCombination(boolean isStartFreeForm) {
        this.mMiuiFreeFormKeyCombinationHelper.freeFormAndFullScreenToggleByKeyCombination(isStartFreeForm);
    }

    public void exitFreeFormByKeyCombination(Task task) {
        this.mMiuiFreeFormKeyCombinationHelper.exitFreeFormByKeyCombination(task);
    }
}
