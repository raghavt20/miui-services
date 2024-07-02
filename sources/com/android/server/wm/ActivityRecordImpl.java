package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.server.LocalServices;
import com.android.server.camera.CameraOpt;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityRecordStub;
import com.android.server.wm.Transition;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import miui.os.DeviceFeature;
import miui.os.MiuiInit;
import miui.security.SecurityManagerInternal;

/* loaded from: classes.dex */
public class ActivityRecordImpl implements ActivityRecordStub {
    private static final ArraySet<String> BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN;
    private static final ArraySet<String> BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN_IN_SPLIT_MODE;
    private static final List<String> COMPAT_WHITE_LIST;
    private static List<String> FORCE_SKIP_RELAUNCH_ACTIVITY_NAMES = null;
    private static final String NON_RESIZEABLE_PORTRAIT_ACTIVITY = "android.server.wm.CompatChangeTests$NonResizeablePortraitActivity";
    public static final String PERMISSION_ACTIVITY = "com.android.packageinstaller.permission.ui.GrantPermissionsActivity";
    public static final String PROPERTY_MIUI_BLUR_CHANGED = "miui.blurChanged";
    public static final String PROPERTY_MIUI_BLUR_RELAUNCH = "miui.blurRelaunch";
    private static final String RESIZEABLE_PORTRAIT_ACTIVITY = "android.server.wm.CompatChangeTests$ResizeablePortraitActivity";
    private static List<String> SKIP_RELAUNCH_ACTIVITY_NAMES = null;
    private static final String STATSD_NON_RESIZEABLE_PORTRAIT_ACTIVITY = "com.android.server.cts.device.statsdatom.StatsdCtsNonResizeablePortraitActivity";
    private static final String SUPPORT_SIZE_CHANGE_PORTRAIT_ACTIVITY = "android.server.wm.CompatChangeTests$SupportsSizeChangesPortraitActivity";
    private static final String TAG = "ActivityRecordImpl";
    private static List<String> sStartWindowBlackList;
    private SecurityManagerInternal mSecurityInternal;

    /* loaded from: classes.dex */
    public static final class MutableActivityRecordImpl implements ActivityRecordStub.MutableActivityRecordStub {
        ActivityRecord mActivityRecord;
        private ActivityTaskManagerService mAtmService;
        private boolean mForcePortraitActivity;
        private boolean mIsCirculatedToVirtual;
        private boolean mIsColdStart;
        private boolean mIsKeyguardEditor;
        private boolean mIsSecSplitAct;
        private boolean mIsSplitMode;
        private AppOrientationInfo mLastAppOrientationInfo;
        private LetterboxUiController mLetterboxUiController;
        private Integer mNavigationBarColor;
        private boolean mNavigationBarColorChanged;
        private boolean mNotRelaunchCirculateApp;

        /* loaded from: classes.dex */
        public final class Provider implements MiuiStubRegistry.ImplProvider<MutableActivityRecordImpl> {

            /* compiled from: ActivityRecordImpl$MutableActivityRecordImpl$Provider.java */
            /* loaded from: classes.dex */
            public static final class SINGLETON {
                public static final MutableActivityRecordImpl INSTANCE = new MutableActivityRecordImpl();
            }

            /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
            public MutableActivityRecordImpl m2419provideSingleton() {
                return SINGLETON.INSTANCE;
            }

            /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
            public MutableActivityRecordImpl m2418provideNewInstance() {
                return new MutableActivityRecordImpl();
            }
        }

        public boolean getIsColdStart() {
            return this.mIsColdStart;
        }

        public void setIsColdStart(boolean isColdStart) {
            this.mIsColdStart = isColdStart;
        }

        public boolean getIsCirculatedToVirtual() {
            return this.mIsCirculatedToVirtual;
        }

        public boolean getNotRelaunchCirculateApp() {
            return this.mNotRelaunchCirculateApp;
        }

        public void setIsCirculatedToVirtual(boolean isCirculatedToVirtual) {
            this.mIsCirculatedToVirtual = isCirculatedToVirtual;
        }

        public void setNotRelaunchCirculateApp(boolean notRelaunchCirculateApp) {
            this.mNotRelaunchCirculateApp = notRelaunchCirculateApp;
        }

        public int adaptCirculateConfigChanged(Task task, int changes, int configChanged) {
            this.mIsCirculatedToVirtual = true;
            return configChanged | changes;
        }

        public void appCirculate(int lastReportedDisplayId, Task task) {
            if (lastReportedDisplayId > 0 && this.mIsCirculatedToVirtual) {
                this.mNotRelaunchCirculateApp = true;
                this.mIsCirculatedToVirtual = false;
            }
            if (task != null) {
                task.mTaskStub.setIsCirculatedToVirtual(this.mIsCirculatedToVirtual);
                task.mTaskStub.setLastDisplayId(lastReportedDisplayId);
            }
        }

        public void init(ActivityRecord record, Intent intent, ActivityTaskManagerService _service, LetterboxUiController letterboxUiController) {
            this.mActivityRecord = record;
            this.mIsSplitMode = ActivityRecordStub.get().initSplitMode(this.mActivityRecord, intent);
            this.mIsSecSplitAct = ActivityRecordStub.get().isSecSplit(this.mIsSplitMode, intent);
            this.mAtmService = _service;
            this.mIsKeyguardEditor = (intent.getMiuiFlags() & 1024) != 0;
            this.mLetterboxUiController = letterboxUiController;
            this.mLastAppOrientationInfo = new AppOrientationInfo();
        }

        public boolean isSplitMode() {
            return this.mIsSplitMode;
        }

        public boolean isKeyguardEditor() {
            return this.mIsKeyguardEditor;
        }

        public boolean isSecSplitAct() {
            return this.mIsSecSplitAct;
        }

        public boolean needSplitAnimation() {
            if (this.mActivityRecord.task == null || !this.mActivityRecord.task.mTaskStub.isSplitMode() || this.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || this.mActivityRecord.inFreeformWindowingMode()) {
                return false;
            }
            return isSplitMode() || ((Boolean) ActivityRecordProxy.mOccludesParent.get(this.mActivityRecord)).booleanValue();
        }

        public boolean shouldSplitBeInvisible(ActivityRecord top) {
            ActivityRecord activityRecord;
            ActivityRecord above;
            return (!isSplitMode() || top == null || top == (activityRecord = this.mActivityRecord) || activityRecord.task == null || (above = this.mActivityRecord.task.getActivityAbove(this.mActivityRecord)) == null || !above.mActivityRecordStub.isSplitMode() || above.finishing) ? false : true;
        }

        public boolean shouldSplitBevisible() {
            return (isSplitMode() && this.mActivityRecord.getState().equals(ActivityRecord.State.STOPPED)) ? false : true;
        }

        public boolean shouldMakeVisible(ActivityRecord top) {
            ActivityRecord activityRecord;
            if (!isSplitMode() || top == (activityRecord = this.mActivityRecord)) {
                return true;
            }
            if (activityRecord.task == null) {
                return false;
            }
            ActivityRecord above = this.mActivityRecord.task.getActivityAbove(this.mActivityRecord);
            if (above == null || above.finishing) {
                return true;
            }
            return (above.mActivityRecordStub.isSplitMode() || ((Boolean) ActivityRecordProxy.mOccludesParent.get(above)).booleanValue()) ? false : true;
        }

        public boolean isDummySecSplitAct() {
            ActivityRecord below = this.mActivityRecord.task != null ? this.mActivityRecord.task.getActivityBelow(this.mActivityRecord) : null;
            return below != null && below.mActivityRecordStub.isSecSplitAct() && below.finishing;
        }

        public boolean isAboveSplitMode() {
            return (this.mActivityRecord.task == null || this.mActivityRecord.task.getActivityAbove(this.mActivityRecord) == null || !this.mActivityRecord.task.getActivityAbove(this.mActivityRecord).mActivityRecordStub.isSplitMode()) ? false : true;
        }

        public boolean isSplitBaseActivity() {
            ActivityRecord ar = this.mAtmService.mLastResumedActivity;
            if (ar != null && ar.mActivityRecordStub.isSplitMode() && ar.getTask() != null && ar.getTask() == this.mActivityRecord.task) {
                ActivityRecord activityRecord = this.mActivityRecord;
                if (activityRecord == activityRecord.task.getRootActivity()) {
                    return true;
                }
            }
            return false;
        }

        public boolean isSecondStateActivity() {
            return isSecSplitAct() || isDummySecSplitAct();
        }

        public String getInitTaskAffinity(ActivityInfo info, String callerName) {
            if (isSplitMode()) {
                String taskAffinity = (info.taskAffinity == null || info.taskAffinity.equals(info.processName)) ? callerName : info.taskAffinity;
                if (info.launchMode == 2) {
                    info.launchMode = 0;
                    return taskAffinity;
                }
                return taskAffinity;
            }
            return info.taskAffinity;
        }

        public void setForcePortraitActivity(boolean forcePortraitActivity) {
            this.mForcePortraitActivity = forcePortraitActivity;
        }

        public boolean isForcePortraitActivity() {
            return this.mForcePortraitActivity;
        }

        public int getNavigationBarColor() {
            Integer num = this.mNavigationBarColor;
            if (num == null) {
                return -1;
            }
            return num.intValue();
        }

        public boolean isInitialColor() {
            return this.mNavigationBarColor == null;
        }

        public void setNavigationBarColor(int color) {
            this.mNavigationBarColorChanged = true;
            this.mNavigationBarColor = Integer.valueOf(color);
        }

        public boolean navigationBarColorChanged() {
            return this.mNavigationBarColorChanged;
        }

        public void updateNavigationBarColorFinish() {
            this.mNavigationBarColorChanged = false;
        }

        public void updateLetterboxSurface(WindowState state) {
            this.mLetterboxUiController.updateLetterboxSurface(state);
        }

        public void setAppOrientation(int type, int orientation) {
            switch (type) {
                case 1:
                    this.mLastAppOrientationInfo.mScreenOrientationInner = orientation;
                    return;
                case 2:
                    this.mLastAppOrientationInfo.mScreenOrientationOuter = orientation;
                    return;
                case 3:
                    this.mLastAppOrientationInfo.mScreenOrientationPad = orientation;
                    return;
                case 4:
                    this.mLastAppOrientationInfo.mFlipScreenOrientationOuter = orientation;
                    return;
                default:
                    return;
            }
        }

        public int getAppOrientation(int type) {
            switch (type) {
                case 1:
                    return this.mLastAppOrientationInfo.mScreenOrientationInner;
                case 2:
                    return this.mLastAppOrientationInfo.mScreenOrientationOuter;
                case 3:
                    return this.mLastAppOrientationInfo.mScreenOrientationPad;
                case 4:
                    return this.mLastAppOrientationInfo.mFlipScreenOrientationOuter;
                default:
                    return -1;
            }
        }

        public boolean isNeedUpdateAppOrientation(int type) {
            switch (type) {
                case 1:
                    return this.mLastAppOrientationInfo.mScreenOrientationInner != -2;
                case 2:
                    return this.mLastAppOrientationInfo.mScreenOrientationOuter != -2;
                case 3:
                    return this.mLastAppOrientationInfo.mScreenOrientationPad != -2;
                case 4:
                    return this.mLastAppOrientationInfo.mFlipScreenOrientationOuter != -2;
                default:
                    return false;
            }
        }

        /* loaded from: classes.dex */
        private static class AppOrientationInfo {
            int mFlipScreenOrientationOuter;
            int mScreenOrientationInner;
            int mScreenOrientationOuter;
            int mScreenOrientationPad;

            private AppOrientationInfo() {
                this.mScreenOrientationInner = -2;
                this.mScreenOrientationOuter = -2;
                this.mScreenOrientationPad = -2;
                this.mFlipScreenOrientationOuter = -2;
            }
        }
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityRecordImpl> {

        /* compiled from: ActivityRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityRecordImpl INSTANCE = new ActivityRecordImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActivityRecordImpl m2421provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActivityRecordImpl m2420provideNewInstance() {
            return new ActivityRecordImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        COMPAT_WHITE_LIST = arrayList;
        ArraySet<String> arraySet = new ArraySet<>();
        BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN = arraySet;
        ArraySet<String> arraySet2 = new ArraySet<>();
        BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN_IN_SPLIT_MODE = arraySet2;
        arraySet.add("com.iflytek.inputmethod.miui/com.iflytek.inputmethod.LauncherSettingsActivity");
        arraySet.add("com.android.camera/.AssistantCamera");
        arraySet.add("com.tencent.mm/.plugin.voip.ui.VideoActivity");
        arraySet.add(AccessController.APP_LOCK_CLASSNAME);
        arraySet.add("com.android.deskclock/.alarm.alert.AlarmAlertFullScreenActivity");
        arraySet.add("tv.danmaku.bili/com.bilibili.video.story.StoryVideoActivity");
        arraySet.add("tv.danmaku.bili/com.bilibili.video.videodetail.VideoDetailsActivity");
        arraySet.add("com.miui.securitycenter/com.miui.applicationlock.TransitionHelper");
        arraySet.add("com.android.incallui/.InCallActivity");
        arraySet.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity");
        arraySet2.add("com.xiaomi.smarthome/.SmartHomeMainActivity");
        arrayList.add(RESIZEABLE_PORTRAIT_ACTIVITY);
        arrayList.add(SUPPORT_SIZE_CHANGE_PORTRAIT_ACTIVITY);
        arrayList.add(NON_RESIZEABLE_PORTRAIT_ACTIVITY);
        arrayList.add(STATSD_NON_RESIZEABLE_PORTRAIT_ACTIVITY);
        sStartWindowBlackList = new ArrayList(Arrays.asList("com.google.android.calendar", ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.xiaomi.market"));
        SKIP_RELAUNCH_ACTIVITY_NAMES = new ArrayList();
        FORCE_SKIP_RELAUNCH_ACTIVITY_NAMES = new ArrayList();
        SKIP_RELAUNCH_ACTIVITY_NAMES.add("com.miui.child.home.home.MainActivity");
        SKIP_RELAUNCH_ACTIVITY_NAMES.add(PERMISSION_ACTIVITY);
        FORCE_SKIP_RELAUNCH_ACTIVITY_NAMES.add("com.android.provision.activities.DefaultActivity");
    }

    static ActivityRecordImpl getInstance() {
        return (ActivityRecordImpl) ActivityRecordStub.get();
    }

    public int addThemeFlag(String packageName) {
        return "com.android.settings".equals(packageName) ? 4194304 : 0;
    }

    public boolean canShowWhenLocked(AppOpsManager appOpsManager, int uid, String packageName, String extra) {
        int mode = appOpsManager.noteOpNoThrow(10020, uid, packageName, (String) null, "ActivityRecordImpl#canShowWhenLocked");
        if (this.mSecurityInternal == null) {
            this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        SecurityManagerInternal securityManagerInternal = this.mSecurityInternal;
        if (securityManagerInternal != null) {
            securityManagerInternal.recordAppBehaviorAsync(36, packageName, 1L, extra);
        }
        if (mode != 0) {
            Slog.i(TAG, "MIUILOG- Show when locked PermissionDenied pkg : " + packageName + " uid : " + uid);
            return false;
        }
        return true;
    }

    public void notifyAppResumedFinished(ActivityRecord r) {
        if (r.mChildren.isEmpty()) {
            r.getDisplayContent().mUnknownAppVisibilityController.appRemovedOrHidden(r);
        } else {
            r.getDisplayContent().mUnknownAppVisibilityController.notifyAppResumedFinished(r);
        }
    }

    public boolean shouldNotBeResume(ActivityRecord r) {
        return r != null && r.getDisplayContent() != null && DeviceFeature.IS_SUBSCREEN_DEVICE && r.getDisplayId() == 2 && r.getDisplayContent().isSleeping() && TextUtils.equals(r.packageName, "com.xiaomi.misubscreenui");
    }

    public boolean allowTaskSnapshot(String pkg, boolean allowTaskSnapshot, TaskSnapshot snapshot, ActivityRecord mActivity) {
        if (AppTransitionInjector.disableSnapshot(pkg) || AppTransitionInjector.disableSnapshotForApplock(pkg, mActivity.getUid()) || AppTransitionInjector.disableSnapshotByComponent(mActivity)) {
            return false;
        }
        if (snapshot == null) {
            return allowTaskSnapshot;
        }
        if (snapshot.getWindowingMode() == 5 && !mActivity.inFreeformWindowingMode()) {
            return false;
        }
        WindowState mainWin = mActivity.findMainWindow();
        if (mainWin != null && mainWin.isSecureLocked()) {
            return false;
        }
        if (MiuiFreezeStub.getInstance().needShowLoading(pkg)) {
            return allowTaskSnapshot;
        }
        if (mActivity.isLetterboxedForFixedOrientationAndAspectRatio() || MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            return false;
        }
        if (mActivity.getTask() == null) {
            return allowTaskSnapshot;
        }
        int taskWidth = mActivity.getTask().getBounds().width();
        int taskHeight = mActivity.getTask().getBounds().height();
        int snapshotWidth = snapshot.getSnapshot().getWidth();
        int snapshotHeight = snapshot.getSnapshot().getHeight();
        if (taskHeight != 0 && snapshotHeight != 0) {
            double difference = Math.abs((taskWidth / taskHeight) - (snapshotWidth / snapshotHeight));
            if (mActivity.getDisplayContent().getRotation() == snapshot.getRotation() && difference > 0.1d) {
                if (!mActivity.inFreeformWindowingMode()) {
                    Slog.d(TAG, "Task bounds and snapshot don't match. task width=" + taskWidth + ", height=" + taskHeight + ": snapshot w=" + snapshotWidth + ", h=" + snapshotHeight + ", difference = " + difference + ", name = " + mActivity);
                    return false;
                }
            }
            return allowTaskSnapshot;
        }
        return allowTaskSnapshot;
    }

    public boolean disableSplashScreen(String componentName) {
        if (BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN.contains(componentName) || MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            return true;
        }
        return false;
    }

    public boolean disableSplashScreenForSplitScreen(String componentName) {
        if (BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN_IN_SPLIT_MODE.contains(componentName)) {
            return true;
        }
        return false;
    }

    public boolean allowShowSlpha(String pkg) {
        if (AppTransitionInjector.ignoreLaunchedFromSystemSurface(pkg)) {
            return false;
        }
        return true;
    }

    public boolean miuiFullscreen(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return !MiuiInit.isRestrictAspect(packageName);
    }

    public boolean isCompatibilityMode() {
        return !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    public boolean isMiuiMwAnimationBelowStack(ActivityRecord activity) {
        return false;
    }

    public String getClassName(ActivityRecord r) {
        if (r != null && r.intent != null && r.intent.getComponent() != null) {
            return r.intent.getComponent().getClassName();
        }
        return null;
    }

    public boolean initSplitMode(ActivityRecord ar, Intent intent) {
        if (intent.isWebIntent()) {
            intent.addMiuiFlags(8);
        }
        boolean isSplitMode = (intent.getMiuiFlags() & 4) != 0 && (intent.getMiuiFlags() & 8) == 0;
        if (isSplitMode) {
            if (ar.occludesParent(true)) {
                ar.setOccludesParent(ar.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || ar.inFreeformWindowingMode());
                return isSplitMode;
            }
            if ((intent.getMiuiFlags() & 16) == 0) {
                intent.addMiuiFlags(8);
                return false;
            }
            return isSplitMode;
        }
        return isSplitMode;
    }

    public boolean isSecSplit(boolean isSplitMode, Intent intent) {
        return isSplitMode && (intent.getMiuiFlags() & 64) != 0;
    }

    public boolean isWorldCirculate(Task task) {
        if (task == null) {
            return false;
        }
        int displayId = task.getDisplayId();
        return displayId != 0 && "com.xiaomi.mirror".equals(getOwnerPackageName(task));
    }

    public String getOwnerPackageName(Task task) {
        DisplayContent display;
        DisplayContent display2;
        ActivityRecord resumedActivity = task.getResumedActivity();
        if (resumedActivity != null && (display2 = resumedActivity.mDisplayContent) != null) {
            String ownerPackageName = display2.mDisplayInfo.ownerPackageName;
            Slog.d(TAG, "displayId = " + task.getDisplayId() + " ownerPackageName = " + ownerPackageName);
            return ownerPackageName;
        }
        ActivityRecord rootActivity = task.getRootActivity();
        if (rootActivity != null && (display = rootActivity.mDisplayContent) != null) {
            String ownerPackageName2 = display.mDisplayInfo.ownerPackageName;
            Slog.d(TAG, "rootActivity = " + rootActivity + "displayId = " + task.getDisplayId() + " ownerPackageName = " + ownerPackageName2);
            return ownerPackageName2;
        }
        return null;
    }

    public void onWindowsVisible(ActivityRecord ar) {
        ActivityTaskManagerServiceImpl atmsImpl = ActivityTaskManagerServiceImpl.getInstance();
        if (atmsImpl != null && atmsImpl.getMiuiSizeCompatIn() != null) {
            atmsImpl.getMiuiSizeCompatIn().showWarningNotification(ar);
        }
    }

    public void updateSpaceToFill(boolean isEmbedded, Rect spaceToFill, Rect windowFrame) {
        if (isEmbedded) {
            spaceToFill.set(spaceToFill.left, windowFrame.top, spaceToFill.right, windowFrame.bottom);
        }
    }

    public void updateLatterboxParam(boolean isEmbedded, Point surfaceOrigin) {
        if (isEmbedded) {
            surfaceOrigin.set(0, 0);
        }
    }

    public int shouldClearActivityInfoFlags() {
        return MiuiPadKeyboardManager.shouldClearActivityInfoFlags();
    }

    public int avoidWhiteScreen(String pkg, int resolvedTheme, String taskAffinity, Task task, boolean taskSwitch, int theme, int mUserId, ActivityRecord record) {
        boolean sameAffinity;
        List<ResolveInfo> ris;
        if (taskAffinity != null && task != null && taskAffinity.equals(task.affinity) && record.getWindowingMode() == 1) {
            sameAffinity = true;
        } else {
            sameAffinity = false;
        }
        if (!taskSwitch || !sameAffinity || sStartWindowBlackList.contains(pkg)) {
            return resolvedTheme;
        }
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.setPackage(pkg);
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        try {
            ris = AppGlobals.getPackageManager().queryIntentActivities(launcherIntent, (String) null, 1L, mUserId).getList();
        } catch (RemoteException e) {
            Slog.d(TAG, "RemoteException when queryIntentActivities");
            ris = new ArrayList<>();
        }
        if (ris.size() != 1) {
            return resolvedTheme;
        }
        ResolveInfo ri = ris.get(0);
        if (ri == null || ri.activityInfo == null || ri.activityInfo.theme == 0) {
            return resolvedTheme;
        }
        if (ri.activityInfo.theme == theme) {
            return resolvedTheme;
        }
        int overrideTheme = ri.activityInfo.theme;
        return overrideTheme;
    }

    public boolean disableSnapshotForApplock(String pkg, int uid) {
        return AppTransitionInjector.disableSnapshotForApplock(pkg, uid);
    }

    public int getOptionsStyle(ActivityOptions options, boolean isLaunchSourceType) {
        int optionStyle = options.getSplashScreenStyle();
        if (optionStyle == -1 && !isLaunchSourceType) {
            return 0;
        }
        return optionStyle;
    }

    public boolean shouldSkipCompatMode(WindowContainer oldParent, WindowContainer newParent) {
        ActivityTaskManagerServiceImpl atmsImpl = ActivityTaskManagerServiceImpl.getInstance();
        if (atmsImpl != null) {
            if (atmsImpl.isInSystemSplitScreen(oldParent) || atmsImpl.isInSystemSplitScreen(newParent)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean skipLetterboxInSplitScreen(ActivityRecord activityRecord) {
        return activityRecord == null || activityRecord.getRootTask() == null || !activityRecord.getRootTask().mSoScRoot;
    }

    public void onArCreated(ActivityRecord record, boolean isUnStandardLaunchMode) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null) {
            miuiHoverIn.onArCreated(record, isUnStandardLaunchMode);
        }
    }

    public void computeHoverModeBounds(Configuration newParentConfiguration, Rect parentBounds, Rect mTmpBounds, ActivityRecord record) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.computeHoverModeBounds(newParentConfiguration, parentBounds, mTmpBounds, record);
        }
    }

    public boolean shouldHookHoverConfig(Configuration newParentConfiguration, ActivityRecord record) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            return miuiHoverIn.shouldHookHoverConfig(newParentConfiguration, record);
        }
        return false;
    }

    public void onArVisibleChanged(ActivityRecord record, boolean visible) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onArVisibleChanged(record, visible);
        }
    }

    public void setRequestedWindowModeForHoverMode(ActivityRecord activityRecord, Configuration newParentConfiguration) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.setRequestedWindowModeForHoverMode(activityRecord, newParentConfiguration);
        }
    }

    public void resetTaskFragmentWindowModeForHoverMode(TaskFragment tf, Configuration newParentConfiguration) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.resetTaskFragmentRequestWindowMode(tf, newParentConfiguration);
        }
    }

    public void setRequestedOrientationForHover(ActivityRecord ar, int requestOrientation, int orientation) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.setOrientationForHoverMode(ar, requestOrientation, orientation);
        }
    }

    public void onArParentChanged(TaskFragment oldParent, TaskFragment newParent, ActivityRecord activityRecord) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onArParentChanged(oldParent, newParent, activityRecord);
        }
    }

    public void onArStateChanged(ActivityRecord ar, ActivityRecord.State state) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.onArStateChanged(ar, state);
        }
        if (ar != null && ar.mActivityComponent != null) {
            CameraOpt.callMethod("notifyActivityStateChange", Integer.valueOf(ar.getPid()), Integer.valueOf(ar.getUid()), ar.mActivityComponent.getPackageName(), ar.mActivityComponent.getClassName(), ar.processName, Integer.valueOf(System.identityHashCode(ar)), Integer.valueOf(state.ordinal()));
        }
    }

    public void enterFreeformForHoverMode(Task task, boolean enter) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null && miuiHoverIn.isDeviceInOpenedOrHalfOpenedState()) {
            miuiHoverIn.enterFreeformForHoverMode(task, enter);
        }
    }

    public boolean shouldRelaunchForBlur(ActivityRecord ar) {
        boolean appBlur;
        PackageManager.Property actProp;
        if (ar == null || ar.mAtmService == null || ar.packageName == null) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        IPackageManager pm = ar.mAtmService.getPackageManager();
        try {
            PackageManager.Property appProp = pm.getPropertyAsUser(PROPERTY_MIUI_BLUR_RELAUNCH, ar.packageName, (String) null, ar.mUserId);
            appBlur = appProp != null && appProp.isBoolean() && appProp.getBoolean();
            actProp = pm.getPropertyAsUser(PROPERTY_MIUI_BLUR_RELAUNCH, ar.packageName, ar.mActivityComponent.getClassName(), ar.mUserId);
        } catch (RemoteException | UnsupportedOperationException e) {
            Slog.e(TAG, e.getMessage());
        }
        if (appBlur && (actProp == null || !actProp.isBoolean())) {
            return true;
        }
        if (actProp != null && actProp.isBoolean()) {
            if (actProp.getBoolean()) {
                return true;
            }
        }
        checkSlow(startTime, 50L, "shouldRelaunchForBlur");
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x0057  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x005c A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:32:? A[ADDED_TO_REGION, RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean shouldSkipChanged(com.android.server.wm.ActivityRecord r12, int r13) {
        /*
            r11 = this;
            java.lang.String r0 = "miui.blurChanged"
            r1 = 0
            if (r12 == 0) goto L75
            com.android.server.wm.ActivityTaskManagerService r2 = r12.mAtmService
            if (r2 == 0) goto L75
            java.lang.String r2 = r12.packageName
            if (r2 == 0) goto L75
            r2 = 1048576(0x100000, float:1.469368E-39)
            r2 = r2 & r13
            if (r2 != 0) goto L14
            goto L75
        L14:
            long r9 = java.lang.System.currentTimeMillis()
            com.android.server.wm.ActivityTaskManagerService r2 = r12.mAtmService
            android.content.pm.IPackageManager r2 = r2.getPackageManager()
            java.lang.String r3 = r12.packageName     // Catch: java.lang.Throwable -> L60
            int r4 = r12.mUserId     // Catch: java.lang.Throwable -> L60
            r5 = 0
            android.content.pm.PackageManager$Property r3 = r2.getPropertyAsUser(r0, r3, r5, r4)     // Catch: java.lang.Throwable -> L60
            r4 = 1
            if (r3 == 0) goto L39
            boolean r5 = r3.isBoolean()     // Catch: java.lang.Throwable -> L60
            if (r5 == 0) goto L39
            boolean r5 = r3.getBoolean()     // Catch: java.lang.Throwable -> L60
            if (r5 != 0) goto L37
            goto L39
        L37:
            r5 = r1
            goto L3a
        L39:
            r5 = r4
        L3a:
            java.lang.String r6 = r12.packageName     // Catch: java.lang.Throwable -> L60
            android.content.ComponentName r7 = r12.mActivityComponent     // Catch: java.lang.Throwable -> L60
            java.lang.String r7 = r7.getClassName()     // Catch: java.lang.Throwable -> L60
            int r8 = r12.mUserId     // Catch: java.lang.Throwable -> L60
            android.content.pm.PackageManager$Property r0 = r2.getPropertyAsUser(r0, r6, r7, r8)     // Catch: java.lang.Throwable -> L60
            if (r0 == 0) goto L59
            boolean r6 = r0.isBoolean()     // Catch: java.lang.Throwable -> L60
            if (r6 == 0) goto L59
            boolean r6 = r0.getBoolean()     // Catch: java.lang.Throwable -> L60
            if (r6 != 0) goto L57
            goto L59
        L57:
            r6 = r1
            goto L5a
        L59:
            r6 = r4
        L5a:
            if (r5 == 0) goto L5f
            if (r6 == 0) goto L5f
            r1 = r4
        L5f:
            return r1
        L60:
            r0 = move-exception
            java.lang.String r3 = "ActivityRecordImpl"
            java.lang.String r4 = r0.getMessage()
            android.util.Slog.e(r3, r4)
            r6 = 50
            java.lang.String r8 = "shouldSkipChanged"
            r3 = r11
            r4 = r9
            r3.checkSlow(r4, r6, r8)
            return r1
        L75:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.ActivityRecordImpl.shouldSkipChanged(com.android.server.wm.ActivityRecord, int):boolean");
    }

    public void adjustSortIfNeeded(ArrayList<Transition.ChangeInfo> targetList, ArrayMap<WindowContainer, Transition.ChangeInfo> changes) {
        ArrayList<ActivityRecord> targetActivityList = new ArrayList<>();
        ArrayList<Integer> needSortIndex = new ArrayList<>();
        for (int i = 0; i < targetList.size(); i++) {
            Transition.ChangeInfo changeInfo = targetList.get(i);
            WindowContainer windowContainer = null;
            Iterator<WindowContainer> it = changes.keySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WindowContainer wc = it.next();
                Transition.ChangeInfo curChangeInfo = changes.get(wc);
                if (curChangeInfo != null && curChangeInfo.equals(changeInfo)) {
                    windowContainer = wc;
                    break;
                }
            }
            ActivityRecord activityRecord = windowContainer != null ? windowContainer.getTopActivity(true, true) : null;
            targetActivityList.add(activityRecord);
            if (activityRecord != null && !activityRecord.fillsParent() && activityRecord.isFinishing() && !activityRecord.mActivityRecordStub.isSplitMode()) {
                needSortIndex.add(Integer.valueOf(i));
            }
        }
        for (int i2 = 0; i2 < needSortIndex.size(); i2++) {
            int index = needSortIndex.get(i2).intValue();
            if (index > 1) {
                Transition.ChangeInfo info = targetList.get(index - 1);
                int mode = info != null ? info.getTransitMode(info.mContainer) : -1;
                if (mode != 1 && mode != 3) {
                    Slog.d(TAG, "Adjust the order of animation targets, " + targetActivityList.get(index) + " need to higher level," + targetActivityList.get(index - 1) + " need  to lower level.");
                    Transition.ChangeInfo tempInfo = targetList.remove(index);
                    targetList.add(index - 1, tempInfo);
                }
            }
        }
    }

    private void checkSlow(long startTime, long threshold, String where) {
        long took = SystemClock.uptimeMillis() - startTime;
        if (took > threshold) {
            Slog.w(TAG, "Slow operation: " + where + " took " + took + "ms.");
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:31:0x00bb, code lost:
    
        if (r17.mType != 2) goto L82;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00c3, code lost:
    
        if (r17.mParticipants.isEmpty() != false) goto L82;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00c5, code lost:
    
        r8 = r17.mParticipants.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00cf, code lost:
    
        if (r8.hasNext() == false) goto L90;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00d1, code lost:
    
        r9 = (com.android.server.wm.WindowContainer) r8.next();
        r11 = r9.asActivityRecord();
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00db, code lost:
    
        if (r11 == null) goto L97;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00df, code lost:
    
        if (r11.finishing == false) goto L98;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00e5, code lost:
    
        if (r11.isLaunchSourceType(2) == false) goto L99;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00eb, code lost:
    
        if (r11.occludesParent(true) != false) goto L100;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00ed, code lost:
    
        if (r21 == null) goto L101;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00f3, code lost:
    
        if (r21.occludesParent() == false) goto L102;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x00f5, code lost:
    
        android.util.Slog.d(com.android.server.wm.ActivityRecordImpl.TAG, "Finish translucent activity start from home , force create open transition.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x00fa, code lost:
    
        return true;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean canStartCollectingNow(int r16, com.android.server.wm.Transition r17, int r18, android.app.ActivityOptions r19, com.android.server.wm.ActivityTaskManagerService r20, com.android.server.wm.ActivityRecord r21) {
        /*
            Method dump skipped, instructions count: 253
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.ActivityRecordImpl.canStartCollectingNow(int, com.android.server.wm.Transition, int, android.app.ActivityOptions, com.android.server.wm.ActivityTaskManagerService, com.android.server.wm.ActivityRecord):boolean");
    }

    public boolean needSkipRelaunch(String activityName, int userId, int changes) {
        return ((SKIP_RELAUNCH_ACTIVITY_NAMES.contains(activityName) && userId != 0) || FORCE_SKIP_RELAUNCH_ACTIVITY_NAMES.contains(activityName)) && (Integer.MIN_VALUE & changes) != 0;
    }

    public boolean isInCompatWhiteList(ActivityInfo info) {
        return COMPAT_WHITE_LIST.contains(info.name);
    }
}
