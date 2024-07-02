package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.animation.Animation;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.wm.ActivityRecord;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MiuiFreeformUtilImpl implements MiuiFreeformUtilStub {
    private static final String TAG = "MiuiFreeformUtilImpl";
    public static final String UNKNOWN = "unknown1";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreeformUtilImpl> {

        /* compiled from: MiuiFreeformUtilImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreeformUtilImpl INSTANCE = new MiuiFreeformUtilImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiFreeformUtilImpl m2541provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiFreeformUtilImpl m2540provideNewInstance() {
            return new MiuiFreeformUtilImpl();
        }
    }

    public void updateApplicationConfiguration(ActivityTaskSupervisor stackSupervisor, Configuration globalConfiguration, String packageName) {
        ActivityTaskSupervisorImpl.updateApplicationConfiguration(stackSupervisor, globalConfiguration, packageName);
    }

    public void setCanStartActivityFromFullscreenToFreefromList(List<String> list) {
        MiuiMultiWindowAdapter.setCanStartActivityFromFullscreenToFreefromList(list);
    }

    public List<String> getCanStartActivityFromFullscreenToFreefromList() {
        return MiuiMultiWindowAdapter.getCanStartActivityFromFullscreenToFreefromList();
    }

    public void setFreeformBlackList(List<String> blackList) {
        MiuiMultiWindowAdapter.setFreeformBlackList(blackList);
    }

    public List<String> getFreeformBlackList() {
        return MiuiMultiWindowAdapter.getFreeformBlackList();
    }

    public List<String> getFreeformBlackList(Context context, HashMap<String, LinkedList<Long>> allTimestamps, boolean isNeedUpdateList) {
        return MiuiMultiWindowUtils.getFreeformBlackList(context, allTimestamps, isNeedUpdateList);
    }

    public void setAbnormalFreeformBlackList(List<String> abnormalFreeformBlackList) {
        MiuiMultiWindowAdapter.setAbnormalFreeformBlackList(abnormalFreeformBlackList);
    }

    public List<String> getAbnormalFreeformBlackList(boolean fromSystem) {
        return MiuiMultiWindowAdapter.getAbnormalFreeformBlackList(fromSystem);
    }

    public void setAbnormalFreeformWhiteList(List<String> abnormalFreeformWhiteList) {
        MiuiMultiWindowAdapter.setAbnormalFreeformWhiteList(abnormalFreeformWhiteList);
    }

    public List<String> getAbnormalFreeformWhiteList(boolean fromSystem) {
        return MiuiMultiWindowAdapter.getAbnormalFreeformWhiteList(fromSystem);
    }

    public void setFreeformVideoWhiteList(List<String> whiteList) {
        MiuiMultiWindowAdapter.setFreeformVideoWhiteList(whiteList);
    }

    public List<String> getFreeformVideoWhiteList() {
        return MiuiMultiWindowAdapter.getFreeformVideoWhiteList();
    }

    public void setFreeformRecommendMapApplicationList(List<String> list) {
        MiuiMultiWindowAdapter.setFreeformRecommendMapApplicationList(list);
    }

    public List<String> getFreeformRecommendMapApplicationList() {
        return MiuiMultiWindowAdapter.getFreeformRecommendMapApplicationList();
    }

    public void setFreeformRecommendWaitingApplicationList(List<String> list) {
        MiuiMultiWindowAdapter.setFreeformRecommendWaitingApplicationList(list);
    }

    public List<String> getFreeformRecommendWaitingApplicationList() {
        return MiuiMultiWindowAdapter.getFreeformRecommendWaitingApplicationList();
    }

    public void setSupportCvwLevelFullList(List<String> list) {
        MiuiMultiWindowAdapter.setSupportCvwLevelFullList(list);
    }

    public List<String> getSupportCvwLevelFullList() {
        return MiuiMultiWindowAdapter.getSupportCvwLevelFullList();
    }

    public void setSupportCvwLevelVerticalList(List<String> list) {
        MiuiMultiWindowAdapter.setSupportCvwLevelVerticalList(list);
    }

    public List<String> getSupportCvwLevelVerticalList() {
        return MiuiMultiWindowAdapter.getSupportCvwLevelVerticalList();
    }

    public void setSupportCvwLevelHorizontalList(List<String> list) {
        MiuiMultiWindowAdapter.setSupportCvwLevelHorizontalList(list);
    }

    public List<String> getSupportCvwLevelHorizontalList() {
        return MiuiMultiWindowAdapter.getSupportCvwLevelHorizontalList();
    }

    public void setUnsupportCvwLevelFullList(List<String> list) {
        MiuiMultiWindowAdapter.setUnsupportCvwLevelFullList(list);
    }

    public List<String> getUnsupportCvwLevelFullList() {
        return MiuiMultiWindowAdapter.getUnsupportCvwLevelFullList();
    }

    public void setLockModeActivityList(List<String> list) {
        MiuiMultiWindowAdapter.setAboutLockModeActivityList(list);
    }

    public List<String> getLockModeActivityList() {
        return MiuiMultiWindowAdapter.getAboutLockModeActivityList();
    }

    public void setFreeformDisableOverlayList(List<String> disableOverlayList) {
        MiuiMultiWindowAdapter.setFreeformDisableOverlayList(disableOverlayList);
    }

    public List<String> getFreeformDisableOverlayList() {
        return MiuiMultiWindowAdapter.getFreeformDisableOverlayList();
    }

    public void setFreeformIgnoreRequestOrientationList(List<String> ignoreRequestOrientationList) {
        MiuiMultiWindowAdapter.setFreeformIgnoreRequestOrientationList(ignoreRequestOrientationList);
    }

    public List<String> getFreeformIgnoreRequestOrientationList() {
        return MiuiMultiWindowAdapter.getFreeformIgnoreRequestOrientationList();
    }

    public void setFreeformNeedRelunchList(List<String> needRelunchList) {
        MiuiMultiWindowAdapter.setFreeformNeedRelunchList(needRelunchList);
    }

    public List<String> getFreeformNeedRelunchList() {
        return MiuiMultiWindowAdapter.getFreeformNeedRelunchList();
    }

    public void setStartFromFreeformBlackList(List<String> startFromFreeformBlackList) {
        MiuiMultiWindowAdapter.setStartFromFreeformBlackList(startFromFreeformBlackList);
    }

    public List<String> getStartFromFreeformBlackList() {
        return MiuiMultiWindowAdapter.getStartFromFreeformBlackList();
    }

    public void setHideSelfIfNewFreeformTaskWhiteList(List<String> hideSelfIfNewFreeformTaskWhiteList) {
        MiuiMultiWindowAdapter.setHideSelfIfNewFreeformTaskWhiteList(hideSelfIfNewFreeformTaskWhiteList);
    }

    public List<String> getHideSelfIfNewFreeformTaskWhiteList() {
        return MiuiMultiWindowAdapter.getHideSelfIfNewFreeformTaskWhiteList();
    }

    public void setShowHiddenTaskIfFinishedWhiteList(List<String> showHiddenTaskIfFinishedWhiteList) {
        MiuiMultiWindowAdapter.setShowHiddenTaskIfFinishedWhiteList(showHiddenTaskIfFinishedWhiteList);
    }

    public List<String> getShowHiddenTaskIfFinishedWhiteList() {
        return MiuiMultiWindowAdapter.getShowHiddenTaskIfFinishedWhiteList();
    }

    public void setFreeformResizeableWhiteList(List<String> resizeableWhiteList) {
        MiuiMultiWindowAdapter.setFreeformResizeableWhiteList(resizeableWhiteList);
    }

    public List<String> getFreeformResizeableWhiteList() {
        return MiuiMultiWindowAdapter.getFreeformResizeableWhiteList();
    }

    public List<String> getCvwUnsupportedFreeformWhiteList() {
        return MiuiMultiWindowAdapter.getCvwUnsupportedFreeformWhiteList();
    }

    public void setCvwUnsupportedFreeformWhiteList(List<String> list) {
        MiuiMultiWindowAdapter.setCvwUnsupportedFreeformWhiteList(list);
    }

    public boolean inFreeformWhiteList(String packageName) {
        return MiuiMultiWindowAdapter.inFreeformWhiteList(packageName);
    }

    public void setApplicationLockActivityList(List<String> applicationLockActivityList) {
        MiuiMultiWindowAdapter.setApplicationLockActivityList(applicationLockActivityList);
    }

    public List<String> getApplicationLockActivityList() {
        return MiuiMultiWindowAdapter.getApplicationLockActivityList();
    }

    public void setFreeformCaptionInsetsHeightToZeroList(List<String> freeformCaptionInsetsHeightToZeroList) {
        MiuiMultiWindowAdapter.setFreeformCaptionInsetsHeightToZeroList(freeformCaptionInsetsHeightToZeroList);
    }

    public List<String> getFreeformCaptionInsetsHeightToZeroList() {
        return MiuiMultiWindowAdapter.getFreeformCaptionInsetsHeightToZeroList();
    }

    public void setForceLandscapeApplication(List<String> forceLandscapeApplication) {
        MiuiMultiWindowAdapter.setForceLandscapeApplication(forceLandscapeApplication);
    }

    public List<String> getForceLandscapeApplication() {
        return MiuiMultiWindowAdapter.getForceLandscapeApplication();
    }

    public void setTopGameList(List<String> topGameList) {
        MiuiMultiWindowAdapter.setTopGameList(topGameList);
    }

    public List<String> getTopGameList() {
        return MiuiMultiWindowAdapter.getTopGameList();
    }

    public void setTopVideoList(List<String> topVideoList) {
        MiuiMultiWindowAdapter.setTopVideoList(topVideoList);
    }

    public List<String> getTopVideoList() {
        return MiuiMultiWindowAdapter.getTopVideoList();
    }

    public void setEnableForegroundPin(boolean enableForegroundPin) {
        MiuiMultiWindowAdapter.setEnableForegroundPin(enableForegroundPin);
    }

    public void setAudioForegroundPinAppList(List<String> audioForegroundPinAppList) {
        MiuiMultiWindowAdapter.setAudioForegroundPinAppList(audioForegroundPinAppList);
    }

    public List<String> getAudioForegroundPinAppList() {
        return MiuiMultiWindowAdapter.getAudioForegroundPinAppList();
    }

    public void setForegroundPinAppWhiteList(List<String> foregroundPinAppWhiteList) {
        MiuiMultiWindowAdapter.setForegroundPinAppWhiteList(foregroundPinAppWhiteList);
    }

    public List<String> getForegroundPinAppWhiteList() {
        return MiuiMultiWindowAdapter.getForegroundPinAppWhiteList();
    }

    public void setForegroundPinAppBlackList(List<String> foregroundPinAppBlackList) {
        MiuiMultiWindowAdapter.setForegroundPinAppBlackList(foregroundPinAppBlackList);
    }

    public List<String> getForegroundPinAppBlackList() {
        return MiuiMultiWindowAdapter.getForegroundPinAppBlackList();
    }

    public void setFixedRotationAppList(List<String> fixedRotationAppList) {
        MiuiMultiWindowAdapter.setFixedRotationAppList(fixedRotationAppList);
    }

    public List<String> getFixedRotationAppList() {
        return MiuiMultiWindowAdapter.getFixedRotationAppList();
    }

    public void setRotationFromDisplayApp(List<String> rotationFromDisplayApp) {
        MiuiMultiWindowAdapter.setRotationFromDisplayApp(rotationFromDisplayApp);
    }

    public List<String> getRotationFromDisplayApp() {
        return MiuiMultiWindowAdapter.getRotationFromDisplayApp();
    }

    public void setUseDefaultCameraPipelineApp(List<String> useDefaultCameraPipelineApp) {
        MiuiMultiWindowAdapter.setUseDefaultCameraPipelineApp(useDefaultCameraPipelineApp);
    }

    public List<String> getUseDefaultCameraPipelineApp() {
        return MiuiMultiWindowAdapter.getUseDefaultCameraPipelineApp();
    }

    public void setSensorDisableWhiteList(List<String> sensorDisableWhiteList) {
        MiuiMultiWindowAdapter.setSensorDisableWhiteList(sensorDisableWhiteList);
    }

    public List<String> getSensorDisableWhiteList() {
        return MiuiMultiWindowAdapter.getSensorDisableWhiteList();
    }

    public void setLaunchInTaskList(List<String> launchInTaskList) {
        MiuiMultiWindowAdapter.setLaunchInTaskList(launchInTaskList);
    }

    public List<String> getLaunchInTaskList() {
        return MiuiMultiWindowAdapter.getLaunchInTaskList();
    }

    public void setEnableAbnormalFreeFormDebug(int enableAbnormalFreeFormDebug) {
        MiuiMultiWindowAdapter.setEnableAbnormalFreeFormDebug(enableAbnormalFreeFormDebug);
    }

    public int getEnableAbnormalFreeFormDebug(boolean fromSystem) {
        return MiuiMultiWindowAdapter.getEnableAbnormalFreeFormDebug(fromSystem);
    }

    public void setSensorDisableList(List<String> sensorDisableList) {
        MiuiMultiWindowAdapter.setSensorDisableList(sensorDisableList);
    }

    public List<String> getSensorDisableList() {
        return MiuiMultiWindowAdapter.getSensorDisableList();
    }

    public void setAdditionalFreeformAspectRatio1Apps(List<String> list) {
        MiuiMultiWindowAdapter.setAdditionalFreeformAspectRatio1Apps(list);
    }

    public List<String> getAdditionalFreeformAspectRatio1Apps() {
        return MiuiMultiWindowAdapter.getAdditionalFreeformAspectRatio1Apps();
    }

    public void setAdditionalFreeformAspectRatio2Apps(List<String> list) {
        MiuiMultiWindowAdapter.setAdditionalFreeformAspectRatio2Apps(list);
    }

    public List<String> getAdditionalFreeformAspectRatio2Apps() {
        return MiuiMultiWindowAdapter.getAdditionalFreeformAspectRatio2Apps();
    }

    public List<String> getDesktopFreeformWhiteList() {
        return MiuiMultiWindowAdapter.getDesktopFreeformWhiteList();
    }

    public void setDesktopFreeformWhiteList(List<String> list) {
        MiuiMultiWindowAdapter.setDesktopFreeformWhiteList(list);
    }

    public boolean supportsFreeform() {
        return ActivityTaskSupervisorImpl.supportsFreeform();
    }

    public boolean notNeedRelunchFreeform(String packageName, Configuration tempConfig, Configuration fullConfig) {
        return MiuiMultiWindowAdapter.notNeedRelunchFreeform(packageName, tempConfig, fullConfig);
    }

    public Rect getFreeformRect(Context context, boolean needDisplayContentRotation, boolean isVertical, boolean isMiniFreeformMode, boolean isFreeformLandscape, Rect outBounds) {
        return MiuiMultiWindowUtils.getFreeformRect(context, needDisplayContentRotation, isVertical, isMiniFreeformMode, isFreeformLandscape, outBounds);
    }

    public ActivityOptions modifyLaunchActivityOptionIfNeed(ActivityTaskManagerService service, RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, Intent intent, int userId, ActivityInfo aInfo, ActivityRecord sourceRecord) {
        return ActivityStarterInjector.modifyLaunchActivityOptionIfNeed(service, root, callingPackgae, options, callerApp, intent, userId, aInfo, sourceRecord);
    }

    public void checkFreeformSupport(ActivityTaskManagerService service, ActivityOptions options) {
        ActivityStarterInjector.checkFreeformSupport(service, options);
    }

    public boolean isEnableAbnormalFreeform(String pkg, Context context, List<String> blackList, List<String> whiteList, int enableAbnormalFreeform) {
        return MiuiMultiWindowUtils.isEnableAbnormalFreeform(pkg, context, blackList, whiteList, enableAbnormalFreeform);
    }

    public boolean isLandscapeGameApp(String pkg, Context context) {
        return MiuiMultiWindowUtils.isLandscapeGameApp(pkg, context);
    }

    public boolean isForceResizeable() {
        return MiuiMultiWindowUtils.isForceResizeable();
    }

    public int handleFreeformModeRequst(IBinder token, int cmd, Context mContext) {
        return ActivityTaskManagerServiceImpl.handleFreeformModeRequst(token, cmd, mContext);
    }

    public Animation loadFreeFormAnimation(WindowManagerService service, int transit, boolean enter, Rect frame, WindowContainer container) {
        return AppTransitionInjector.loadFreeFormAnimation(service, transit, enter, frame, container);
    }

    public float getMiuiMultiWindowUtilsScale() {
        return MiuiMultiWindowUtils.sScale;
    }

    public void setMiuiMultiWindowUtilsScale(float scale) {
        MiuiMultiWindowUtils.sScale = scale;
    }

    public boolean isOrientationLandscape(int orientation) {
        return MiuiMultiWindowUtils.isOrientationLandscape(orientation);
    }

    public int getTopDecorCaptionViewHeight() {
        return MiuiMultiWindowUtils.TOP_DECOR_CAPTIONVIEW_HEIGHT;
    }

    public float getFreeformRoundCorner() {
        return MiuiMultiWindowUtils.FREEFORM_ROUND_CORNER;
    }

    public void adjuestScaleAndFrame(WindowState win, Task task) {
        WindowStateStubImpl.adjuestScaleAndFrame(win, task);
    }

    public void adjuestFrameForChild(WindowState win) {
        WindowStateStubImpl.adjuestFrameForChild(win);
    }

    public void adjuestFreeFormTouchRegion(WindowState win, Region outRegion) {
        WindowStateStubImpl.adjuestFreeFormTouchRegion(win, outRegion);
    }

    public void onForegroundWindowChanged(WindowProcessController app, ActivityInfo info, ActivityRecord record, ActivityRecord.State state) {
        ActivityTaskManagerServiceImpl.onForegroundWindowChanged(app, info, record, state);
    }

    public boolean isMiuiBuild() {
        return SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    public boolean isUseFreeFormAnimation(int transit) {
        return AppTransitionInjector.isUseFreeFormAnimation(transit);
    }

    public boolean isMiuiCvwFeatureEnable() {
        return false;
    }

    public float getShadowRadius() {
        return 400.0f;
    }

    public boolean isDisableSplashScreenInFreeFormTaskSwitch(String componentName) {
        return "com.tencent.qqlive/com.tencent.tauth.AuthActivity".equals(componentName);
    }

    public boolean ignoreClearTaskFlagInFreeForm(ActivityRecord reusedActivity, ActivityRecord startActivity) {
        return (reusedActivity == null || startActivity == null || (reusedActivity.shortComponentName == null && startActivity.shortComponentName == null) || !"com.tencent.mobileqq/com.tencent.open.agent.AgentActivity".equals(reusedActivity.shortComponentName) || !"com.tencent.mobileqq/.activity.LoginActivity".equals(startActivity.shortComponentName)) ? false : true;
    }

    public boolean isSkipUpdateFreeFormShadow(Task task) {
        return task != null && task.getChildCount() == 1 && task.getTopNonFinishingActivity() != null && "com.eg.android.AlipayGphone/com.alipay.mobile.quinox.SchemeLauncherActivity".equals(task.getTopNonFinishingActivity().shortComponentName);
    }

    public boolean isPadScreen(Context context) {
        return MiuiMultiWindowUtils.isPadScreen(context);
    }

    public boolean multiFreeFormSupported(Context context) {
        return MiuiMultiWindowUtils.multiFreeFormSupported(context);
    }

    public float getHotSpaceOffset() {
        return MiuiMultiWindowUtils.HOT_SPACE_OFFSITE;
    }

    public float getHotSpaceBottomOffsetPad() {
        return MiuiMultiWindowUtils.HOT_SPACE_BOTTOM_OFFSITE_PAD;
    }

    public float getMiniFreeformPaddingStroke() {
        return 20.0f;
    }

    public boolean needRelunchFreeform(String packageName, Configuration tempConfig, Configuration fullConfig) {
        return MiuiMultiWindowAdapter.needRelunchFreeform(packageName, tempConfig, fullConfig);
    }

    public boolean shouldSkipRelaunchForDkt(String activityName) {
        return MiuiMultiWindowAdapter.shouldSkipRelaunchForDkt(activityName);
    }

    public int getHotSpaceResizeOffsetPad() {
        return 33;
    }

    public int getHotSpaceTopCaptionUpwardsOffset() {
        return 22;
    }

    public float reviewFreeFormBounds(Rect currentBounds, Rect newBounds, float currentScale, Rect accessibleArea) {
        return MiuiMultiWindowUtils.reviewFreeFormBounds(currentBounds, newBounds, currentScale, accessibleArea);
    }

    public Rect getFreeFormAccessibleArea(Context context, DisplayContent displayContent) {
        if (displayContent == null) {
            return new Rect();
        }
        InsetsStateController insetsStateController = displayContent.getInsetsStateController();
        return MiuiMultiWindowUtils.getFreeFormAccessibleArea(context, displayContent.getRotation(), MiuiFreeFormGestureController.getStatusBarHeight(insetsStateController), MiuiFreeFormGestureController.getNavBarHeight(insetsStateController), MiuiFreeFormGestureController.getDisplayCutoutHeight(displayContent.mDisplayFrames), MiuiDesktopModeUtils.isActive(context));
    }

    public float getFreeformScale(ActivityOptions options) {
        Method method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0]);
        if (method != null) {
            try {
                return ((Float) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformScale", new Object[0])).floatValue();
            } catch (Exception e) {
                e.printStackTrace();
                return 1.0f;
            }
        }
        return 1.0f;
    }

    public boolean isEnalbleAbnormalFreeform(Context context, String packageName) {
        return (isLandscapeGameApp(packageName, context) || isPadScreen(context)) && (isEnableAbnormalFreeform(packageName, context, getAbnormalFreeformBlackList(true), getAbnormalFreeformWhiteList(true), getEnableAbnormalFreeFormDebug(true)) || isDeskTopModeActive());
    }

    public ActivityRecord adjustTopActivityIfNeed(ActivityRecord oriTop) {
        if (oriTop != null && oriTop.getRootTask() != null && oriTop.getRootTask().inFreeformWindowingMode() && oriTop.getRootTask().isAlwaysOnTop()) {
            ActivityRecord topAr = oriTop.getDisplayArea().topRunningActivity(false, true);
            Slog.d(TAG, " adjustTopActivityIfNeed topAr: " + topAr);
            return topAr;
        }
        return oriTop;
    }

    public boolean setFreeformScale(float scale, ActivityOptions options) {
        Method method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0]);
        if (method != null) {
            try {
                return MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "setFreeformScale", new Object[]{Float.valueOf(scale)}) != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isMIUIProduct() {
        return Build.IS_MIUI;
    }

    public boolean isSupportFreeFormMultiTask(String packageName) {
        return MiuiMultiWindowAdapter.isSupportFreeFormMultiTask(packageName);
    }

    public void initDesktop(Context context) {
        MiuiDesktopModeUtils.initDesktop(context);
    }

    public boolean isDeskTopModeActive() {
        return MiuiDesktopModeUtils.isDesktopActive();
    }

    public boolean isSupportFreeFormInDesktopMode(Task task) {
        String packageName;
        if (task == null) {
            return false;
        }
        if (task.origActivity != null) {
            packageName = task.origActivity.getPackageName();
        } else if (task.realActivity != null) {
            packageName = task.realActivity.getPackageName();
        } else {
            packageName = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : "unknown";
        }
        ActivityRecord r = task.topRunningActivityLocked();
        if (getDesktopModeLaunchFullscreenAppList().contains(packageName) || (r != null && getDesktopModeLaunchFullscreenAppList().contains(r.shortComponentName))) {
            return false;
        }
        if (r == null && getDesktopModeLaunchFullscreenAppList().contains(task.realActivity.flattenToShortString())) {
            return false;
        }
        ActivityRecord fullScreenActivity = task.getActivity(new Predicate() { // from class: com.android.server.wm.MiuiFreeformUtilImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MiuiFreeformUtilImpl.lambda$isSupportFreeFormInDesktopMode$0((ActivityRecord) obj);
            }
        });
        if (fullScreenActivity != null) {
            return true;
        }
        return true ^ task.mLaunchFullScreenInDesktopMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isSupportFreeFormInDesktopMode$0(ActivityRecord activityRecord) {
        if (activityRecord.isFullScreen()) {
            return true;
        }
        return false;
    }

    public List<String> getDesktopModeLaunchFullscreenAppList() {
        List<String> list = MiuiMultiWindowAdapter.getDesktopModeLaunchFullscreenAppList();
        return list != null ? list : new ArrayList();
    }

    public void setDesktopModeLaunchFullscreenAppList(List<String> needRelunchList) {
        MiuiMultiWindowAdapter.setDesktopModeLaunchFullscreenAppList(needRelunchList);
    }

    public List<String> getDesktopModeLaunchFullscreenNotHideOtherAppList() {
        List<String> list = MiuiMultiWindowAdapter.getDesktopModeLaunchFullscreenNotHideOtherAppList();
        return list != null ? list : new ArrayList();
    }

    public void setDesktopModeLaunchFullscreenNotHideOtherAppList(List<String> needRelunchList) {
        MiuiMultiWindowAdapter.setDesktopModeLaunchFullscreenNotHideOtherAppList(needRelunchList);
    }

    public void setDesktopModeLaunchFreeformIgnoreTranslucentAppList(List<String> list) {
        MiuiMultiWindowAdapter.setDesktopModeLaunchFreeformIgnoreTranslucentAppList(list);
    }

    public List<String> getDesktopModeLaunchFreeformIgnoreTranslucentAppList() {
        return MiuiMultiWindowAdapter.getDesktopModeLaunchFreeformIgnoreTranslucentAppList();
    }

    public boolean isFullScreenStrategyNeededInDesktopMode(ActivityRecord r, int windowingMode) {
        if (!isDeskTopModeActive() || windowingMode != 0 || r == null) {
            return false;
        }
        if (!getDesktopModeLaunchFullscreenAppList().contains(r.packageName) && !getDesktopModeLaunchFullscreenAppList().contains(r.shortComponentName)) {
            return false;
        }
        Slog.i(TAG, " isFullScreenStrategyNeededInDesktopMode, launch fullscreen, r.shortComponentName= " + r.shortComponentName);
        return true;
    }

    public boolean isFullScreenStrategyNeededInDesktopMode(String shortComponentName) {
        if (!isDeskTopModeActive() || !getDesktopModeLaunchFullscreenAppList().contains(shortComponentName)) {
            return false;
        }
        Slog.i(TAG, " isFullScreenStrategyNeededInDesktopMode, launch fullscreen, shortComponentName= " + shortComponentName);
        return true;
    }

    public boolean isLaunchNotesInKeyGuard(Intent intent) {
        if (!isDeskTopModeActive() || intent == null) {
            return false;
        }
        Bundle bundle = intent.getExtras();
        String scene = bundle != null ? bundle.getString("scene") : "";
        return MiuiStylusShortcutManager.SCENE_KEYGUARD.equals(scene) || MiuiStylusShortcutManager.SCENE_OFF_SCREEN.equals(scene);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v1 */
    /* JADX WARN: Type inference failed for: r1v2, types: [int] */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.StringBuilder] */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.StringBuilder] */
    /* JADX WARN: Type inference failed for: r3v0 */
    /* JADX WARN: Type inference failed for: r3v1, types: [int] */
    /* JADX WARN: Type inference failed for: r3v2 */
    /* JADX WARN: Type inference failed for: r3v3 */
    public boolean isAppSetAutoUiInDesktopMode(ActivityRecord r, int windowingMode, Context context) {
        if (!isDeskTopModeActive() || windowingMode != 0 || r == null) {
            return false;
        }
        ?? r1 = -1;
        ?? r3 = -1;
        try {
            PackageManager.Property propertyAutouiApplication = context.getPackageManager().getProperty("android.window.PROPERTY_AUTOUI_ALLOW_SYSTEM_OVERRIDE", r.packageName);
            if (propertyAutouiApplication.getName().isEmpty()) {
                r1 = -1;
            } else {
                r1 = propertyAutouiApplication.getBoolean();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            PackageManager.Property propertyAutouiComponent = context.getPackageManager().getProperty("android.window.PROPERTY_AUTOUI_ALLOW_SYSTEM_OVERRIDE", r.mActivityComponent);
            if (propertyAutouiComponent.getName().isEmpty()) {
                r3 = -1;
            } else {
                r3 = propertyAutouiComponent.getBoolean();
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
        if (r3 != 1 && (r1 != 1 || r3 == 0)) {
            return false;
        }
        Slog.i(TAG, "autouiAllow:: r.packageName=" + r.packageName + ", r.mActivityComponent =" + r.mActivityComponent + ", autouiAllowApplication=" + r1 + ", autouiAllowComponent=" + r3);
        return true;
    }

    public boolean isAppLockActivity(ComponentName componentName, ComponentName cls) {
        return MiuiMultiWindowAdapter.isAppLockActivity(componentName, cls);
    }

    public SafeActivityOptions setSafeActivityOptions(Task task) {
        String packageName;
        if (task == null || !isDeskTopModeActive() || !task.inFreeformWindowingMode()) {
            return null;
        }
        if (!isSupportFreeFormInDesktopMode(task)) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchWindowingMode(1);
            return new SafeActivityOptions(options);
        }
        if (task.origActivity != null) {
            packageName = task.origActivity.getPackageName();
        } else if (task.realActivity != null) {
            packageName = task.realActivity.getPackageName();
        } else {
            packageName = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : "unknown";
        }
        return new SafeActivityOptions(MiuiMultiWindowUtils.getActivityOptions(task.mAtmService.mContext, packageName, true, false, isDeskTopModeActive()));
    }
}
