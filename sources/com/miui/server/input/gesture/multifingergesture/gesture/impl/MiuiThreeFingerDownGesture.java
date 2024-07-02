package com.miui.server.input.gesture.multifingergesture.gesture.impl;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiuiThreeFingerDownGesture extends BaseMiuiMultiFingerGesture {
    private static final int HORIZONTAL_THRESHOLD = 24;
    private float mThreshold;

    public MiuiThreeFingerDownGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        super(context, handler, manager);
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onTouchEvent(MotionEvent event) {
        handleEvent(event);
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public String getGestureKey() {
        return "three_gesture_down";
    }

    private void handleEvent(MotionEvent event) {
        if (event.getAction() != 2) {
            return;
        }
        float distanceX = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        float distanceY = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            distanceY += event.getY(i) - this.mInitY[i];
            distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
        }
        if (distanceY < (-this.mThreshold)) {
            Slog.w(this.TAG, "Gesture fail, because " + distanceY + " < " + (-this.mThreshold));
            checkFail();
        } else if (Build.IS_TABLET && distanceX > 24.0f && distanceX > distanceY) {
            Slog.w(this.TAG, "Gesture fail, because " + distanceX + " > " + distanceY);
            checkFail();
        } else if (distanceY >= this.mThreshold) {
            checkSuccess();
        }
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onConfigChange() {
        super.onConfigChange();
        updateConfig();
    }

    private void updateConfig() {
        this.mThreshold = getFunctionNeedFingerNum() * this.mContext.getResources().getDisplayMetrics().density * 50.0f * (Build.IS_TABLET ? 2 : 1);
    }
}
