package com.miui.server.input.gesture.multifingergesture.gesture.impl;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import android.view.InputDevice;
import android.view.MotionEvent;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import miui.os.Build;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class MiuiThreeFingerLongPressGesture extends BaseMiuiMultiFingerGesture {
    private static final boolean IS_FLIP_DEVICE = MiuiMultiDisplayTypeInfo.isFlipDevice();
    private static final int THREE_LONG_PRESS_TIME_OUT = 600;
    private static final int XIAOMI_TOUCHPAD_PRODUCT_ID = 161;
    private static final int XIAOMI_TOUCHPAD_VENDOR_ID = 5593;
    private float mMoveThreshold;
    private final Runnable mThreeLongPressRunnable;

    public MiuiThreeFingerLongPressGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        super(context, handler, manager);
        this.mThreeLongPressRunnable = new Runnable() { // from class: com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerLongPressGesture$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiThreeFingerLongPressGesture.this.checkSuccess();
            }
        };
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onTouchEvent(MotionEvent event) {
        handleEvent(event);
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public String getGestureKey() {
        return "three_gesture_long_press";
    }

    private void handleEvent(MotionEvent event) {
        if (event.getAction() == 2) {
            float distanceX = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            float distanceY = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
                distanceY += Math.abs(event.getY(i) - this.mInitY[i]);
                distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
            }
            float f = this.mMoveThreshold;
            if (distanceX >= f || distanceY >= f) {
                checkFail();
                return;
            }
            return;
        }
        if (event.getActionMasked() == 5) {
            this.mHandler.postDelayed(this.mThreeLongPressRunnable, 600L);
        }
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    protected void onFail() {
        this.mHandler.removeCallbacks(this.mThreeLongPressRunnable);
    }

    private void updateConfig() {
        this.mMoveThreshold = this.mContext.getResources().getDisplayMetrics().density * 50.0f * (Build.IS_TABLET ? 2 : 1) * 0.6f;
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onConfigChange() {
        super.onConfigChange();
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public boolean preCondition() {
        if (this.mMiuiMultiFingerGestureManager.isKeyguardActive()) {
            return false;
        }
        return (IS_FLIP_DEVICE && this.mMiuiMultiFingerGestureManager.getIsFolded()) ? false : true;
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void initGesture(MotionEvent event) {
        InputDevice device = event.getDevice();
        if (device == null) {
            Slog.d(this.TAG, "This motion event device is null");
        } else if (device.getVendorId() == XIAOMI_TOUCHPAD_VENDOR_ID && device.getProductId() == 161) {
            checkFail();
            return;
        }
        super.initGesture(event);
    }
}
