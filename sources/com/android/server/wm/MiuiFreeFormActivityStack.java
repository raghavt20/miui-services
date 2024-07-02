package com.android.server.wm;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import com.miui.server.AccessController;
import miui.app.MiuiFreeFormManager;

/* loaded from: classes.dex */
public class MiuiFreeFormActivityStack extends MiuiFreeFormActivityStackStub {
    private final String TAG;
    boolean isNormalFreeForm;
    int mCornerPosition;
    private final Rect mEnterMiniFreeformFromRect;
    private String mEnterMiniFreeformReason;
    int mFreeFormLaunchFromTaskId;
    float mFreeFormScale;
    private boolean mHadHideStackFromFullScreen;
    boolean mHasHadStackAdded;
    boolean mIsForegroundPin;
    boolean mIsFrontFreeFormStackInfo;
    boolean mIsLandcapeFreeform;
    int mMiuiFreeFromWindowMode;
    private boolean mNeedAnimation;
    Rect mPinFloatingWindowPos;
    boolean mShouldDelayDispatchFreeFormStackModeChanged;
    Task mTask;
    long pinActiveTime;
    long timestamp;

    void onMiuiFreeFormStasckAdded() {
    }

    public boolean isNeedAnimation() {
        return this.mNeedAnimation;
    }

    public void setNeedAnimation(boolean needAnimation) {
        this.mNeedAnimation = needAnimation;
    }

    public Rect getEnterMiniFreeformRect() {
        return new Rect(this.mEnterMiniFreeformFromRect);
    }

    public String getEnterMiniFreeformReason() {
        return this.mEnterMiniFreeformReason;
    }

    public void setEnterMiniFreeformReason(String enterMiniFreeformReason) {
        this.mEnterMiniFreeformReason = enterMiniFreeformReason;
    }

    public void setEnterMiniFreeformRect(Rect enterMiniFreeformFromRect) {
        if (enterMiniFreeformFromRect == null) {
            return;
        }
        this.mEnterMiniFreeformFromRect.set(enterMiniFreeformFromRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inPinMode() {
        int i = this.mMiuiFreeFromWindowMode;
        return i == 2 || i == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inNormalPinMode() {
        return this.mMiuiFreeFromWindowMode == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inMiniPinMode() {
        return this.mMiuiFreeFromWindowMode == 3;
    }

    public void setCornerPosition(int cornerPosition) {
        this.mCornerPosition = cornerPosition;
    }

    public boolean isLandcapeFreeform() {
        return this.mIsLandcapeFreeform;
    }

    public void setStackFreeFormMode(int mode) {
        this.mMiuiFreeFromWindowMode = mode;
    }

    public int getMiuiFreeFromWindowMode() {
        return this.mMiuiFreeFromWindowMode;
    }

    public boolean isInMiniFreeFormMode() {
        return this.mMiuiFreeFromWindowMode == 1;
    }

    public boolean isInFreeFormMode() {
        return this.mMiuiFreeFromWindowMode == 0;
    }

    public float getFreeFormScale() {
        return this.mFreeFormScale;
    }

    public void setFreeformScale(float scale) {
        this.mFreeFormScale = scale;
    }

    public int getFreeFormLaunchFromTaskId() {
        return this.mFreeFormLaunchFromTaskId;
    }

    public boolean isNormalFreeForm() {
        return this.isNormalFreeForm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreeformTimestamp(long lastTimestamp) {
        this.timestamp = lastTimestamp;
    }

    public MiuiFreeFormActivityStack(Task task) {
        this(task, 0);
    }

    public MiuiFreeFormActivityStack(Task task, int miuiFreeFromWindowMode) {
        this.TAG = "MiuiFreeFormActivityStack";
        this.mHasHadStackAdded = false;
        this.mNeedAnimation = true;
        this.mHadHideStackFromFullScreen = false;
        this.mIsFrontFreeFormStackInfo = true;
        this.mPinFloatingWindowPos = new Rect();
        this.mCornerPosition = -1;
        this.mEnterMiniFreeformFromRect = new Rect();
        this.isNormalFreeForm = true;
        this.mShouldDelayDispatchFreeFormStackModeChanged = false;
        this.mFreeFormLaunchFromTaskId = 0;
        this.mTask = task;
        this.isNormalFreeForm = task.mWmService.mAtmService.mMiuiFreeFormManagerService.isNormalFreeForm(task, getStackPackageName());
        ActivityRecord top = task.getTopNonFinishingActivity(false);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setPackage(getStackPackageName());
        ResolveInfo rInfo = task.mAtmService.mContext.getPackageManager().resolveActivity(intent, 0);
        if (top != null) {
            this.mIsLandcapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(top.getOverrideOrientation());
        } else if (rInfo != null) {
            this.mIsLandcapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(rInfo.activityInfo.screenOrientation);
        }
        this.mIsLandcapeFreeform |= MiuiMultiWindowAdapter.getForceLandscapeApplication().contains(getStackPackageName());
        setStackFreeFormMode(miuiFreeFromWindowMode);
        if (!this.mTask.mTaskSupervisor.getLaunchParamsController().hasFreeformDesktopMemory(task)) {
            this.mFreeFormScale = MiuiMultiWindowUtils.getOriFreeformScale(task.mAtmService.mContext, this.mIsLandcapeFreeform, this.isNormalFreeForm, MiuiDesktopModeUtils.isActive(task.mAtmService.mContext), getStackPackageName());
        } else {
            this.mFreeFormScale = this.mTask.mTaskSupervisor.getLaunchParamsController().getFreeformLastScale(this.mTask);
        }
        this.mHadHideStackFromFullScreen = false;
        this.mShouldDelayDispatchFreeFormStackModeChanged = false;
    }

    public int getUid() {
        Task task = this.mTask;
        if (task != null && task.realActivity != null && AccessController.APP_LOCK_CLASSNAME.contains(this.mTask.realActivity.flattenToString()) && this.mTask.originatingUid != 0) {
            return this.mTask.originatingUid;
        }
        return this.mTask.mUserId;
    }

    public String getStackPackageName() {
        Task task = this.mTask;
        if (task == null) {
            return null;
        }
        if (task.realActivity != null && AccessController.APP_LOCK_CLASSNAME.contains(this.mTask.realActivity.flattenToString()) && this.mTask.behindAppLockPkg != null && this.mTask.behindAppLockPkg.length() > 0) {
            return this.mTask.behindAppLockPkg;
        }
        if (this.mTask.origActivity != null) {
            return this.mTask.origActivity.getPackageName();
        }
        if (this.mTask.realActivity != null) {
            return this.mTask.realActivity.getPackageName();
        }
        if (this.mTask.getTopActivity(false, true) != null) {
            return this.mTask.getTopActivity(false, true).packageName;
        }
        return null;
    }

    public String getApplicationName() {
        ApplicationInfo applicationInfo;
        PackageManager packageManager = null;
        try {
            packageManager = this.mTask.mAtmService.mContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(getStackPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return "";
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("MiuiFreeFormActivityStack{");
        info.append(" mStackID=" + this.mTask.getRootTaskId());
        info.append(" mPackageName=" + getStackPackageName());
        info.append(" mFreeFormScale=" + this.mFreeFormScale);
        info.append(" mMiuiFreeFromWindowMode=" + this.mMiuiFreeFromWindowMode);
        info.append(" mConfiguration=" + this.mTask.getConfiguration());
        info.append(" mNeedAnimation=" + this.mNeedAnimation);
        info.append(" mIsLandcapeFreeform=" + this.mIsLandcapeFreeform);
        info.append(" mPinFloatingWindowPos=" + this.mPinFloatingWindowPos);
        info.append(" isNormalFreeForm=" + this.isNormalFreeForm);
        info.append("mHadHideStackFromFullScreen= " + this.mHadHideStackFromFullScreen);
        info.append("mIsFrontFreeFormStackInfo= " + this.mIsFrontFreeFormStackInfo);
        info.append(" timestamp=" + this.timestamp);
        if (this.mTask != null) {
            info.append(" mTask.inFreeformWindowingMode()=" + this.mTask.inFreeformWindowingMode());
            info.append(" mTask.originatingUid=" + this.mTask.originatingUid);
        }
        info.append('}');
        return info.toString();
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getMiuiFreeFormStackInfo() {
        MiuiFreeFormManager.MiuiFreeFormStackInfo freeFormStackInfo = new MiuiFreeFormManager.MiuiFreeFormStackInfo();
        freeFormStackInfo.stackId = this.mTask.getRootTaskId();
        freeFormStackInfo.bounds = this.mTask.getBounds();
        freeFormStackInfo.windowState = this.mMiuiFreeFromWindowMode;
        freeFormStackInfo.packageName = getStackPackageName();
        freeFormStackInfo.displayId = this.mTask.getDisplayId();
        freeFormStackInfo.userId = getUid();
        freeFormStackInfo.configuration = this.mTask.getConfiguration();
        int i = this.mMiuiFreeFromWindowMode;
        if (i == -1) {
            freeFormStackInfo.smallWindowBounds = new Rect();
        } else if (i == 1) {
            Rect smallWindowBounds = new Rect(freeFormStackInfo.bounds);
            int left = smallWindowBounds.left;
            int top = smallWindowBounds.top;
            smallWindowBounds.scale(this.mFreeFormScale);
            smallWindowBounds.offsetTo(left, top);
            freeFormStackInfo.smallWindowBounds = smallWindowBounds;
        }
        freeFormStackInfo.freeFormScale = this.mFreeFormScale;
        freeFormStackInfo.inPinMode = inPinMode();
        freeFormStackInfo.isForegroundPin = this.mIsForegroundPin;
        freeFormStackInfo.pinFloatingWindowPos = this.mPinFloatingWindowPos;
        freeFormStackInfo.isNormalFreeForm = this.isNormalFreeForm;
        freeFormStackInfo.cornerPosition = this.mCornerPosition;
        freeFormStackInfo.enterMiniFreeformFromRect = new Rect(this.mEnterMiniFreeformFromRect);
        freeFormStackInfo.enterMiniFreeformReason = this.mEnterMiniFreeformReason;
        freeFormStackInfo.isLandcapeFreeform = this.mIsLandcapeFreeform;
        freeFormStackInfo.timestamp = this.timestamp;
        freeFormStackInfo.hadHideStackFormFullScreen = this.mHadHideStackFromFullScreen;
        freeFormStackInfo.needAnimation = this.mNeedAnimation;
        return freeFormStackInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHideStackFromFullScreen() {
        return this.mHadHideStackFromFullScreen;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHideStackFromFullScreen(boolean hidden) {
        this.mHadHideStackFromFullScreen = hidden;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsFrontFreeFormStackInfo(boolean isFrontFreeFormStackInfo) {
        this.mIsFrontFreeFormStackInfo = isFrontFreeFormStackInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFrontFreeFormStackInfo() {
        return this.mIsFrontFreeFormStackInfo;
    }
}
