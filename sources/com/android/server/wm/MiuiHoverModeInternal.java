package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import com.android.server.wm.ActivityRecord;

/* loaded from: classes.dex */
public class MiuiHoverModeInternal {
    public void onSystemReady(WindowManagerService wms) {
    }

    public void onArCreated(ActivityRecord record, boolean isUnStandardLaunchMode) {
    }

    public boolean isDeviceInOpenedOrHalfOpenedState() {
        return false;
    }

    public void computeHoverModeBounds(Configuration newParentConfiguration, Rect parentBounds, Rect mTmpBounds, ActivityRecord record) {
    }

    public boolean shouldHookHoverConfig(Configuration newParentConfiguration, ActivityRecord record) {
        return false;
    }

    public void onArVisibleChanged(ActivityRecord record, boolean visible) {
    }

    public void updateLastRotation(DisplayContent dc, int lastRotation) {
    }

    public void setRequestedWindowModeForHoverMode(ActivityRecord activityRecord, Configuration newParentConfiguration) {
    }

    public void resetTaskFragmentRequestWindowMode(TaskFragment tf, Configuration newParentConfiguration) {
    }

    public void setOrientationForHoverMode(ActivityRecord ar, int requestOrientation, int orientation) {
    }

    public void onHoverModeRecentAnimStart() {
    }

    public void onFinishTransition() {
    }

    public void onArParentChanged(TaskFragment oldParent, TaskFragment newParent, ActivityRecord activityRecord) {
    }

    public void onHoverModeTaskParentChanged(Task task, WindowContainer newParent) {
    }

    public void onArStateChanged(ActivityRecord ar, ActivityRecord.State state) {
    }

    public void onDisplayOverrideConfigUpdate(DisplayContent displayContent) {
    }

    public void onHoverModeTaskPrepareSurfaces(Task task) {
    }

    public void updateHoverGuidePanel(WindowState windowState, boolean add) {
    }

    public void adaptLetterboxInsets(ActivityRecord activity, Rect letterboxInsets) {
    }

    public void onActivityPipModeChangedForHoverMode(boolean inPip, ActivityRecord ar) {
    }

    public void onStopFreezingDisplayLocked() {
    }

    public void calcHoverAnimatingColor(float[] startColor) {
    }

    public void onScreenRotationAnimationEnd() {
    }

    public void enterFreeformForHoverMode(Task task, boolean enter) {
    }

    public int getHoverModeRecommendRotation(DisplayContent displayContent) {
        return -1;
    }

    public boolean shouldHoverModeEnableSensor(DisplayContent displayContent) {
        return false;
    }

    public void onTaskConfigurationChanged(int prevWindowingMode, int overrideWindowingMode) {
    }
}
