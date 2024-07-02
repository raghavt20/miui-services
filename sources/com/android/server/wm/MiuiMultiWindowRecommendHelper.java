package com.android.server.wm;

import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import miui.os.Build;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class MiuiMultiWindowRecommendHelper {
    private static final String MULTI_WINDOW_RECOMMEND_SWITCH = "MiuiMultiWindowRecommendSwitch";
    public static final int RECENT_APP_LIST_SIZE = 4;
    private static final int RECOMMEND_SWITCH_ENABLE_STATE = 1;
    private static final int RECOMMEND_SWITCH_STATELESS = -1;
    private static final String TAG = "MiuiMultiWindowRecommendHelper";
    private Context mContext;
    MiuiFreeFormManagerService mFreeFormManagerService;
    private boolean mLastIsWideScreen;
    MiuiMultiWindowRecommendController mMiuiMultiWindowRecommendController;
    private MultiWindowRecommendReceiver mMultiWindowRecommendReceiver;
    private List<SplitScreenRecommendTaskInfo> mRecentAppList = new ArrayList(4);
    private long mMaxTimeFrame = 120000;
    private volatile long lastSplitScreenRecommendTime = 0;
    private volatile boolean mMultiWindowRecommendSwitchEnabled = false;
    private int mRecentAppListMaxSize = 4;
    private RecommendDataEntry mSpiltScreenRecommendDataEntry = new RecommendDataEntry();
    private RecommendDataEntry mFreeFormRecommendDataEntry = new RecommendDataEntry();
    private boolean mFirstWindowHasDraw = false;
    private Configuration mLastConfiguration = new Configuration();
    final Object mLock = new Object();
    final Object mRecentAppListLock = new Object();
    private IForegroundInfoListener.Stub listener = new IForegroundInfoListener.Stub() { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper.1
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            String focusedTaskPackageName;
            if (!MiuiMultiWindowRecommendHelper.this.isDeviceSupportSplitScreenRecommend() || MiuiDesktopModeUtils.isDesktopActive()) {
                return;
            }
            MiuiMultiWindowRecommendHelper.this.mMiuiMultiWindowRecommendController.removeFreeFormRecommendView();
            Task focusedTask = MiuiMultiWindowRecommendHelper.this.getFocusedTask();
            if (focusedTask != null && focusedTask.realActivity != null) {
                String focusedTaskPackageName2 = focusedTask.realActivity.getPackageName();
                focusedTaskPackageName = focusedTaskPackageName2;
            } else {
                focusedTaskPackageName = null;
            }
            if (focusedTaskPackageName != null && focusedTaskPackageName.equals(foregroundInfo.mForegroundPackageName) && MiuiMultiWindowRecommendHelper.this.checkPreConditionsForSplitScreen(focusedTask)) {
                MiuiMultiWindowRecommendHelper.this.predictSplitScreen(new SplitScreenRecommendTaskInfo(focusedTask.mTaskId, focusedTaskPackageName, System.currentTimeMillis(), focusedTask));
                Slog.d(MiuiMultiWindowRecommendHelper.TAG, " focused task id = " + focusedTask.getRootTaskId() + " packageName = " + focusedTaskPackageName);
            }
        }
    };
    private final TaskStackListener mTaskStackListener = new TaskStackListener() { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper.2
        public void onTaskRemoved(int taskId) {
            if (!MiuiMultiWindowRecommendHelper.this.isDeviceSupportSplitScreenRecommend()) {
                return;
            }
            if (MiuiMultiWindowRecommendHelper.this.inSplitScreenRecommendState() && MiuiMultiWindowRecommendHelper.this.mSpiltScreenRecommendDataEntry != null && (MiuiMultiWindowRecommendHelper.this.mSpiltScreenRecommendDataEntry.getPrimaryTaskId() == taskId || MiuiMultiWindowRecommendHelper.this.mSpiltScreenRecommendDataEntry.getSecondaryTaskId() == taskId)) {
                Slog.d(MiuiMultiWindowRecommendHelper.TAG, " onTaskRemoved: remove SplitScreenRecommendView, taskId= " + taskId);
                MiuiMultiWindowRecommendHelper.this.mMiuiMultiWindowRecommendController.removeSplitScreenRecommendView();
            }
            if (MiuiMultiWindowRecommendHelper.this.inFreeFormRecommendState() && MiuiMultiWindowRecommendHelper.this.mFreeFormRecommendDataEntry != null && MiuiMultiWindowRecommendHelper.this.mFreeFormRecommendDataEntry.getFreeFormTaskId() == taskId) {
                Slog.d(MiuiMultiWindowRecommendHelper.TAG, " onTaskRemoved: remove FreeFormRecommendView, taskId= " + taskId);
                MiuiMultiWindowRecommendHelper.this.mMiuiMultiWindowRecommendController.removeFreeFormRecommendView();
            }
        }
    };
    private ContentObserver mMuiltiWindowRecommendObserver = new ContentObserver(new Handler((Handler.Callback) null, false)) { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper.3
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            ContentResolver contentResolver = MiuiMultiWindowRecommendHelper.this.mContext.getContentResolver();
            MiuiMultiWindowRecommendHelper.this.mMultiWindowRecommendSwitchEnabled = Settings.System.getIntForUser(contentResolver, MiuiMultiWindowRecommendHelper.MULTI_WINDOW_RECOMMEND_SWITCH, -1, -2) == 1;
            Slog.d(MiuiMultiWindowRecommendHelper.TAG, " MultiWindowRecommendSwitch change,new mMultiWindowRecommendSwitchEnabled= " + MiuiMultiWindowRecommendHelper.this.mMultiWindowRecommendSwitchEnabled);
        }
    };
    private SplitScreenRecommendPredictHelper mSplitScreenRecommendPredictHelper = new SplitScreenRecommendPredictHelper(this);

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiuiMultiWindowRecommendHelper(Context context, MiuiFreeFormManagerService service) {
        this.mContext = context;
        this.mFreeFormManagerService = service;
        MultiWindowRecommendReceiver multiWindowRecommendReceiver = new MultiWindowRecommendReceiver();
        this.mMultiWindowRecommendReceiver = multiWindowRecommendReceiver;
        this.mContext.registerReceiver(multiWindowRecommendReceiver, multiWindowRecommendReceiver.mFilter, null, this.mFreeFormManagerService.mHandler);
        this.mMiuiMultiWindowRecommendController = new MiuiMultiWindowRecommendController(this.mContext, this.mFreeFormManagerService.mActivityTaskManagerService.mWindowManager, this);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MULTI_WINDOW_RECOMMEND_SWITCH), false, this.mMuiltiWindowRecommendObserver, -2);
        initMultiWindowRecommendSwitchState();
        registerForegroundInfoListener();
        registerTaskStackListener();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceSupportSplitScreenRecommend() {
        if (this.mMultiWindowRecommendSwitchEnabled && RecommendUtils.isSupportMiuiMultiWindowRecommend() && MiuiMultiWindowUtils.isSupportSplitScreenFeature()) {
            return Build.IS_TABLET || MiuiMultiWindowUtils.isFoldInnerScreen(this.mContext);
        }
        return false;
    }

    private void initMultiWindowRecommendSwitchState() {
        try {
            if (RecommendUtils.isSupportMiuiMultiWindowRecommend()) {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                boolean z = true;
                if (Settings.System.getIntForUser(contentResolver, MULTI_WINDOW_RECOMMEND_SWITCH, -1, -2) == -1) {
                    this.mMultiWindowRecommendSwitchEnabled = true;
                } else {
                    if (Settings.System.getIntForUser(contentResolver, MULTI_WINDOW_RECOMMEND_SWITCH, -1, -2) != 1) {
                        z = false;
                    }
                    this.mMultiWindowRecommendSwitchEnabled = z;
                }
            }
            Slog.d(TAG, " initMultiWindowRecommendSwitchState mMultiWindowRecommendSwitchEnabled= " + this.mMultiWindowRecommendSwitchEnabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* loaded from: classes.dex */
    private final class MultiWindowRecommendReceiver extends BroadcastReceiver {
        private static final String ACTION_FREE_FORM_RECOMMEND = "com.miui.freeform_recommend";
        private static final String RECOMMEND_SCENE = "recommendScene";
        private static final String RECOMMEND_TRANSACTION_TYPE = "recommendTransactionType";
        private static final String SENDER_PACKAGENAME = "senderPackageName";
        IntentFilter mFilter;

        public MultiWindowRecommendReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            this.mFilter = intentFilter;
            intentFilter.addAction(ACTION_FREE_FORM_RECOMMEND);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Bundle bundle;
            if (!MiuiMultiWindowRecommendHelper.this.isDeviceSupportFreeFormRecommend()) {
                return;
            }
            String action = intent.getAction();
            if (!ACTION_FREE_FORM_RECOMMEND.equals(action) || (bundle = intent.getExtras()) == null) {
                return;
            }
            String senderPackageName = bundle.getString(SENDER_PACKAGENAME);
            int recommendTransactionType = bundle.getInt(RECOMMEND_TRANSACTION_TYPE);
            int recommendScene = bundle.getInt(RECOMMEND_SCENE);
            Slog.d(MiuiMultiWindowRecommendHelper.TAG, "onReceive: senderPackageName= " + senderPackageName + " recommendTransactionType= " + recommendTransactionType + " recommendScene= " + recommendScene);
            MiuiMultiWindowRecommendHelper.this.FreeFormRecommendIfNeeded(senderPackageName, recommendTransactionType, recommendScene);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceSupportFreeFormRecommend() {
        return this.mMultiWindowRecommendSwitchEnabled && RecommendUtils.isSupportMiuiMultiWindowRecommend() && MiuiMultiWindowUtils.supportFreeform();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void FreeFormRecommendIfNeeded(String senderPackageName, int recommendTransactionType, int recommendScene) {
        if (senderPackageName == null) {
            return;
        }
        if (recommendTransactionType != 1 && recommendTransactionType != 3) {
            return;
        }
        if (recommendScene != 1 && recommendScene != 2) {
            return;
        }
        if (!MiuiMultiWindowAdapter.LIST_ABOUT_FREEFORM_RECOMMEND_WAITING_APPLICATION.contains(senderPackageName)) {
            Slog.d(TAG, "FreeFormRecommendIfNeeded senderPackageName is not in LIST_ABOUT_FREEFORM_RECOMMEND_WAITING_APPLICATION ");
            return;
        }
        if (this.mFreeFormManagerService.mActivityTaskManagerService.isInSplitScreenWindowingMode()) {
            Slog.d(TAG, "FreeFormRecommendIfNeeded isInSplitScreenWindowingMode ");
            return;
        }
        Task currentFullRootTask = this.mFreeFormManagerService.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
        if (currentFullRootTask == null || !senderPackageName.equals(currentFullRootTask.getPackageName())) {
            Slog.d(TAG, "FreeFormRecommendIfNeeded senderPackageName not equals currentFullRootTask.getPackageName() ");
            return;
        }
        if (recommendTransactionType == 3) {
            this.mMiuiMultiWindowRecommendController.removeFreeFormRecommendView();
            return;
        }
        if (checkPreConditionsForFreeForm()) {
            if (inSplitScreenRecommendState()) {
                this.mMiuiMultiWindowRecommendController.removeSplitScreenRecommendView();
            }
            RecommendDataEntry buildFreeFormRecommendDataEntry = buildFreeFormRecommendDataEntry(senderPackageName, recommendTransactionType, recommendScene, currentFullRootTask.getRootTaskId(), currentFullRootTask.mUserId);
            this.mFreeFormRecommendDataEntry = buildFreeFormRecommendDataEntry;
            buildAndAddFreeFormRecommendView(buildFreeFormRecommendDataEntry);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void predictSplitScreen(SplitScreenRecommendTaskInfo splitScreenRecommendTaskInfo) {
        List<SplitScreenRecommendTaskInfo> frequentSwitchedTask;
        Slog.d(TAG, "current foreground task: taskId is" + splitScreenRecommendTaskInfo.getTaskId() + " package name: " + splitScreenRecommendTaskInfo.getPkgName());
        synchronized (this.mRecentAppListLock) {
            updateAppDataList(splitScreenRecommendTaskInfo);
            frequentSwitchedTask = this.mSplitScreenRecommendPredictHelper.getFrequentSwitchedTask(this.mRecentAppList);
        }
        if (frequentSwitchedTask != null) {
            if (hasNotification()) {
                Slog.d(TAG, "predictSplitScreen hasNotification ");
                setLastSplitScreenRecommendTime(System.currentTimeMillis());
                clearRecentAppList();
            } else {
                RecommendDataEntry buildSpiltScreenRecommendDataEntry = buildSpiltScreenRecommendDataEntry(frequentSwitchedTask, splitScreenRecommendTaskInfo);
                this.mSpiltScreenRecommendDataEntry = buildSpiltScreenRecommendDataEntry;
                buildAndAddSpiltScreenRecommendViewIfNeeded(buildSpiltScreenRecommendDataEntry);
            }
        }
    }

    private RecommendDataEntry buildSpiltScreenRecommendDataEntry(List<SplitScreenRecommendTaskInfo> frequentSwitchedTaskList, SplitScreenRecommendTaskInfo splitScreenRecommendTaskInfo) {
        RecommendDataEntry dataEntry = new RecommendDataEntry();
        dataEntry.setTransactionType(2);
        dataEntry.setRecommendSceneType(3);
        int currentTaskId = splitScreenRecommendTaskInfo.getTaskId();
        for (SplitScreenRecommendTaskInfo taskInfo : frequentSwitchedTaskList) {
            if (taskInfo.getTaskId() == currentTaskId) {
                dataEntry.setPrimaryPackageName(taskInfo.getPkgName());
                dataEntry.setPrimaryTaskId(taskInfo.getTaskId());
                dataEntry.setPrimaryUserId(taskInfo.getTask().mUserId);
            } else {
                dataEntry.setSecondaryPackageName(taskInfo.getPkgName());
                dataEntry.setSecondaryTaskId(taskInfo.getTaskId());
                dataEntry.setSecondaryUserId(taskInfo.getTask().mUserId);
            }
        }
        return dataEntry;
    }

    private RecommendDataEntry buildFreeFormRecommendDataEntry(String senderPackageName, int recommendTransactionType, int recommendScene, int taskId, int userId) {
        RecommendDataEntry dataEntry = new RecommendDataEntry();
        dataEntry.setTransactionType(recommendTransactionType);
        dataEntry.setRecommendSceneType(recommendScene);
        dataEntry.setFreeformPackageName(senderPackageName);
        dataEntry.setFreeFormTaskId(taskId);
        dataEntry.setFreeformUserId(userId);
        return dataEntry;
    }

    private void buildAndAddSpiltScreenRecommendViewIfNeeded(RecommendDataEntry recommendDataEntry) {
        long timeoutAtTimeMs = System.currentTimeMillis() + 1000;
        synchronized (this.mLock) {
            this.mFirstWindowHasDraw = false;
            while (this.mFirstWindowHasDraw) {
                try {
                    long waitMillis = timeoutAtTimeMs - System.currentTimeMillis();
                    if (waitMillis <= 0) {
                        break;
                    } else {
                        this.mLock.wait(waitMillis);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        buildAndAddSpiltScreenRecommendView(recommendDataEntry);
    }

    private void buildAndAddSpiltScreenRecommendView(RecommendDataEntry recommendDataEntry) {
        setLastSplitScreenRecommendTime(System.currentTimeMillis());
        clearRecentAppList();
        this.mMiuiMultiWindowRecommendController.removeSplitScreenRecommendView();
        this.mMiuiMultiWindowRecommendController.addSplitScreenRecommendView(recommendDataEntry);
    }

    private void buildAndAddFreeFormRecommendView(RecommendDataEntry recommendDataEntry) {
        this.mMiuiMultiWindowRecommendController.removeFreeFormRecommendView();
        this.mMiuiMultiWindowRecommendController.addFreeFormRecommendView(recommendDataEntry);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastSplitScreenRecommendTime(long recommendTime) {
        this.lastSplitScreenRecommendTime = recommendTime;
        Slog.d(TAG, " setLastSplitScreenRecommendTime= " + recommendTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkPreConditionsForSplitScreen(Task task) {
        if (task == null || task.isActivityTypeHomeOrRecents()) {
            return false;
        }
        if (task.getWindowingMode() != 1 || RecommendUtils.isInSplitScreenWindowingMode(task)) {
            Slog.d(TAG, " task window mode is " + task.getWindowingMode() + " isInSplitScreenWindowingMode= " + RecommendUtils.isInSplitScreenWindowingMode(task));
            clearRecentAppList();
            return false;
        }
        if (!inSplitScreenRecommendState()) {
            return true;
        }
        Slog.d(TAG, " InSplitScreenRecommendState ");
        clearRecentAppList();
        return false;
    }

    private boolean checkPreConditionsForFreeForm() {
        if (hasNotification()) {
            Slog.d(TAG, "checkPreConditionsForFreeForm  hasNotification");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean inSplitScreenRecommendState() {
        if (this.mMiuiMultiWindowRecommendController.inSplitScreenRecommendState()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean inFreeFormRecommendState() {
        if (this.mMiuiMultiWindowRecommendController.inFreeFormRecommendState()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSplitScreenRecommendValid(RecommendDataEntry recommendDataEntry) {
        if (recommendDataEntry == null) {
            return false;
        }
        synchronized (this.mFreeFormManagerService.mActivityTaskManagerService.mGlobalLock) {
            Task primaryTask = this.mFreeFormManagerService.mActivityTaskManagerService.mRootWindowContainer.getRootTask(recommendDataEntry.getPrimaryTaskId());
            Task secondaryTask = this.mFreeFormManagerService.mActivityTaskManagerService.mRootWindowContainer.getRootTask(recommendDataEntry.getSecondaryTaskId());
            if (primaryTask != null && secondaryTask != null) {
                return true;
            }
            Slog.d(TAG, " SplitScreenRecommend invalid ");
            return false;
        }
    }

    private void updateAppDataList(SplitScreenRecommendTaskInfo splitScreenRecommendTaskInfo) {
        addNewTaskToRecentAppList(splitScreenRecommendTaskInfo);
        removeTimeOutTasks();
        removeExcessTasks();
        printRecentAppInfo();
    }

    private void addNewTaskToRecentAppList(SplitScreenRecommendTaskInfo splitScreenRecommendTaskInfo) {
        if (this.mRecentAppList.size() > 1) {
            for (int i = 0; i < this.mRecentAppList.size(); i++) {
                SplitScreenRecommendTaskInfo taskInfo = this.mRecentAppList.get(i);
                if (taskInfo.getTask() == splitScreenRecommendTaskInfo.getTask()) {
                    Slog.d(TAG, " addNewTaskToRecentAppList task id = " + splitScreenRecommendTaskInfo.getTaskId());
                    this.mRecentAppList.add(splitScreenRecommendTaskInfo);
                    return;
                }
            }
            this.mRecentAppList.remove(0);
        }
        Slog.d(TAG, " addNewTaskToRecentAppList task id = " + splitScreenRecommendTaskInfo.getTaskId());
        this.mRecentAppList.add(splitScreenRecommendTaskInfo);
    }

    private void removeTimeOutTasks() {
        if (!this.mRecentAppList.isEmpty()) {
            final long switchTime = this.mRecentAppList.get(r0.size() - 1).getSwitchTime();
            long count = this.mRecentAppList.stream().filter(new Predicate() { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper.4
                @Override // java.util.function.Predicate
                public boolean test(Object obj) {
                    return switchTime - ((SplitScreenRecommendTaskInfo) obj).getSwitchTime() > MiuiMultiWindowRecommendHelper.this.getMaxTimeFrame();
                }
            }).count();
            for (int i = 0; i < count; i++) {
                SplitScreenRecommendTaskInfo remove = this.mRecentAppList.remove(0);
                Slog.d(TAG, "remove task exceed max time limit, taskId is " + remove.getTaskId() + " switchTime= " + remove.getSwitchTime());
            }
        }
    }

    private void removeExcessTasks() {
        int size = this.mRecentAppList.size() - this.mRecentAppListMaxSize;
        if (size > 0) {
            Slog.d(TAG, "excess task size is " + size);
            for (int i = 0; i < size; i++) {
                this.mRecentAppList.remove(0);
            }
        }
    }

    public long getMaxTimeFrame() {
        return this.mMaxTimeFrame;
    }

    private void printRecentAppInfo() {
        String recentAppInfo = (String) this.mRecentAppList.stream().map(new Function() { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String pkgName;
                pkgName = ((SplitScreenRecommendTaskInfo) obj).getPkgName();
                return pkgName;
            }
        }).collect(Collectors.joining(","));
        Slog.d(TAG, "printRecentAppInfo: " + recentAppInfo + " mRecentAppList size= " + this.mRecentAppList.size());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearRecentAppList() {
        synchronized (this.mRecentAppListLock) {
            if (this.mRecentAppList.isEmpty()) {
                return;
            }
            this.mRecentAppList.clear();
            Slog.d(TAG, "clear recent app list");
        }
    }

    private void registerForegroundInfoListener() {
        ProcessManager.registerForegroundInfoListener(this.listener);
    }

    private void unregisterForegroundInfoListener() {
        ProcessManager.unregisterForegroundInfoListener(this.listener);
    }

    private void registerTaskStackListener() {
        this.mFreeFormManagerService.mActivityTaskManagerService.registerTaskStackListener(this.mTaskStackListener);
    }

    private void unregisterTaskStackListener() {
        this.mFreeFormManagerService.mActivityTaskManagerService.unregisterTaskStackListener(this.mTaskStackListener);
    }

    public Task getFocusedTask() {
        DisplayContent displayContent = this.mFreeFormManagerService.mActivityTaskManagerService.mWindowManager.getDefaultDisplayContentLocked();
        if (displayContent == null || displayContent.mFocusedApp == null) {
            return null;
        }
        Task focusedTask = displayContent.mFocusedApp.getTask();
        return focusedTask;
    }

    public boolean hasNotification() {
        boolean z;
        synchronized (this.mFreeFormManagerService.mActivityTaskManagerService.getGlobalLock()) {
            WindowState notificationShade = this.mFreeFormManagerService.mActivityTaskManagerService.mRootWindowContainer.getDefaultDisplay().getDisplayPolicy().getNotificationShade();
            z = notificationShade != null && notificationShade.isVisible();
        }
        return z;
    }

    public void onFirstWindowDrawn(ActivityRecord activityRecord) {
        if (activityRecord == null || activityRecord.getTask() == null) {
            return;
        }
        int taskId = activityRecord.getTask().getRootTaskId();
        synchronized (this.mLock) {
            RecommendDataEntry recommendDataEntry = this.mSpiltScreenRecommendDataEntry;
            if (recommendDataEntry != null && recommendDataEntry.getPrimaryTaskId() == taskId && !this.mFirstWindowHasDraw) {
                Slog.d(TAG, "onFirstWindowDrawn: taskId = " + taskId + " activityRecord: " + activityRecord);
                this.mFirstWindowHasDraw = true;
                this.mLock.notifyAll();
            }
        }
    }

    public int startSmallFreeformFromNotification() {
        Task currentFullTask;
        if (RecommendUtils.isKeyguardLocked(this.mContext) || !RecommendUtils.isSupportMiuiMultiWindowRecommend() || (currentFullTask = this.mFreeFormManagerService.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1)) == null || currentFullTask.realActivity == null || RecommendUtils.isInSplitScreenWindowingMode(currentFullTask)) {
            return 2;
        }
        String packageName = this.mFreeFormManagerService.getStackPackageName(currentFullTask);
        Slog.d(TAG, "startSmallFreeformFromNotification: currentFullTask packageName= " + packageName);
        if (!MiuiMultiWindowAdapter.LIST_ABOUT_FREEFORM_RECOMMEND_MAP_APPLICATION.contains(packageName) && !MiuiMultiWindowAdapter.LIST_ABOUT_FREEFORM_RECOMMEND_MAP_APPLICATION.contains(currentFullTask.realActivity.flattenToShortString())) {
            return 2;
        }
        MiuiMultiWindowUtils.invoke(this.mFreeFormManagerService.mActivityTaskManagerService, "launchMiniFreeFormWindowVersion2", new Object[]{Integer.valueOf(currentFullTask.getRootTaskId()), 2, "enterSmallFreeFormByNotificationRecommend", new Rect()});
        return 1;
    }

    public void displayConfigurationChange(final DisplayContent displayContent, final Configuration configuration) {
        this.mFreeFormManagerService.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendHelper.5
            @Override // java.lang.Runnable
            public void run() {
                Slog.d(MiuiMultiWindowRecommendHelper.TAG, "displayConfigurationChange: configuration= " + configuration);
                if (displayContent.getDisplayId() != 0) {
                    return;
                }
                boolean isWideScreen = MiuiMultiWindowUtils.isWideScreen(configuration);
                if (MiuiMultiWindowRecommendHelper.this.mLastIsWideScreen != isWideScreen) {
                    MiuiMultiWindowRecommendHelper.this.mLastIsWideScreen = isWideScreen;
                    if (!isWideScreen) {
                        MiuiMultiWindowRecommendHelper.this.mMiuiMultiWindowRecommendController.removeSplitScreenRecommendView();
                    }
                }
                int changes = MiuiMultiWindowRecommendHelper.this.mLastConfiguration.updateFrom(configuration);
                boolean orientationChange = (changes & 128) != 0;
                if (orientationChange) {
                    Slog.d(MiuiMultiWindowRecommendHelper.TAG, "orientationChange");
                    MiuiMultiWindowRecommendHelper.this.mMiuiMultiWindowRecommendController.removeRecommendView();
                }
            }
        });
    }
}
