package com.miui.server.input.stylus.laser;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.display.DisplayViewport;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.input.config.InputCommonConfig;
import com.miui.server.input.stylus.StylusOneTrackHelper;
import java.util.List;

/* loaded from: classes.dex */
public class LaserPointerController implements PointerControllerInterface {
    private static final int DIRTY_KEEP_PATH = 4;
    private static final int DIRTY_MODE = 2;
    private static final int DIRTY_POSITION = 1;
    private static final int DIRTY_PRESENTATION_VISIBLE = 16;
    private static final int DIRTY_VISIBLE = 8;
    private static final long KEY_DOWN_TIMEOUT = 240000;
    private static final long LONG_PRESS_TIMEOUT = 200;
    private static final long MULTI_PRESS_TIMEOUT = 300;
    private static final long POKE_USER_ACTIVITY_TIMEOUT = 2000;
    private static final long REMOVE_WINDOW_TIME = 120000;
    private static final String TAG = "LaserPointerController";
    private static final long TIMEOUT_TIME = 120000;
    private boolean mCanShowLaser;
    private final Context mContext;
    private DisplayViewport mDisplayViewport;
    private boolean mHandleByKeyDownTimeout;
    private boolean mHandleByLongPressing;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final InputCommonConfig mInputCommonConfig;
    private boolean mIsDownFromScreenOff;
    private boolean mIsKeyDown;
    private boolean mIsScreenOn;
    private final LaserState mLaserState;
    private LaserView mLaserView;
    private long mLastDownTime;
    private WindowManager.LayoutParams mLayoutParams;
    private final Object mLock;
    private final MiuiInputManagerInternal mMiuiInputManagerInternal;
    private final PowerManager mPowerManager;
    private int mPressCounter;
    private final LaserState mTempLaserState;
    private final PointF mTempPosition;
    private final WindowManager mWindowManger;
    private int mDisplayId = 0;
    private long mLastRequestFadeTime = -120000;
    private long mLastPokeUserActivityTime = -2000;

    public LaserPointerController() {
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mContext = systemContext;
        this.mWindowManger = (WindowManager) systemContext.getSystemService(WindowManager.class);
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(handlerThread.getLooper());
        this.mLock = new Object();
        this.mLaserState = new LaserState();
        this.mTempPosition = new PointF();
        this.mTempLaserState = new LaserState();
        this.mPowerManager = (PowerManager) systemContext.getSystemService(PowerManager.class);
        this.mInputCommonConfig = InputCommonConfig.getInstance();
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void setPosition(float x, float y) {
        synchronized (this.mLock) {
            setPositionLocked(x, y, false);
        }
    }

    private void setPositionLocked(float x, float y, boolean force) {
        if (!canShowLaserViewLocked() && !force) {
            return;
        }
        this.mTempPosition.set(x, y);
        if (!movePointInScreenLocked(this.mTempPosition) && this.mTempPosition.equals(this.mLaserState.mPosition)) {
            return;
        }
        this.mLaserState.mPosition.set(this.mTempPosition.x, this.mTempPosition.y);
        invalidateLocked(1);
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void getPosition(float[] outPosition) {
        synchronized (this.mLock) {
            outPosition[0] = this.mLaserState.mPosition.x;
            outPosition[1] = this.mLaserState.mPosition.y;
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void move(float deltaX, float deltaY) {
        synchronized (this.mLock) {
            float newX = this.mLaserState.mPosition.x + deltaX;
            float newY = this.mLaserState.mPosition.y + deltaY;
            setPositionLocked(newX, newY, false);
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void fade(int transition) {
        synchronized (this.mLock) {
            fadeLocked(transition);
        }
    }

    private void fadeLocked(int transition) {
        if (!this.mLaserState.mVisible) {
            return;
        }
        this.mLaserState.mVisible = false;
        invalidateLocked(8);
        if (this.mLaserState.mCurrentMode == 1) {
            setModeLocked(0);
            setKeepPathLocked(false);
        }
        fadePresentationLocked(0);
        this.mCanShowLaser = false;
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void unfade(int transition) {
        synchronized (this.mLock) {
            unfadeLocked(transition);
        }
    }

    private void unfadeLocked(int transition) {
        if (this.mLaserState.mVisible || !canShowLaserViewLocked()) {
            return;
        }
        this.mLaserState.mVisible = true;
        invalidateLocked(8);
    }

    private void fadePresentationLocked(int transition) {
        if (!this.mLaserState.mPresentationVisible) {
            return;
        }
        this.mLaserState.mPresentationVisible = false;
        invalidateLocked(16);
        this.mLastRequestFadeTime = System.currentTimeMillis();
    }

    private void unfadePresentationLocked(int transition) {
        unfadeLocked(transition);
        if (this.mLaserState.mPresentationVisible) {
            return;
        }
        this.mLaserState.mPresentationVisible = true;
        invalidateLocked(16);
        if (System.currentTimeMillis() - this.mLastRequestFadeTime > 120000) {
            resetPositionLocked();
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void setDisplayViewPort(List<DisplayViewport> viewports) {
        synchronized (this.mLock) {
            DisplayViewport displayViewport = findDisplayViewportById(viewports, this.mDisplayId);
            if (displayViewport == null) {
                Slog.w(TAG, "Can't find the designated viewport with ID " + this.mDisplayId + " to update laser input mapper. Fall back to default display");
                displayViewport = findDisplayViewportById(viewports, 0);
            }
            if (displayViewport == null) {
                Slog.e(TAG, "Still can't find a viable viewport to update cursor input mapper. Skip setting it to LaserPointerController.");
                return;
            }
            Slog.i(TAG, "Display viewport updated, new display viewport = " + displayViewport);
            this.mDisplayViewport = displayViewport;
            fadeLocked(0);
            setPositionLocked(displayViewport.logicalFrame.width() / 2.0f, displayViewport.logicalFrame.height() / 2.0f, true);
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void setDisplayId(int displayId) {
        Slog.i(TAG, "DisplayId update, new displayId = " + displayId + ", old displayId = " + this.mDisplayId);
        synchronized (this.mLock) {
            this.mDisplayId = displayId;
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public void resetPosition() {
        synchronized (this.mLock) {
            resetPositionLocked();
        }
    }

    @Override // com.miui.server.input.stylus.laser.PointerControllerInterface
    public boolean canShowPointer() {
        boolean canShowLaserViewLocked;
        synchronized (this.mLock) {
            canShowLaserViewLocked = canShowLaserViewLocked();
        }
        return canShowLaserViewLocked;
    }

    private void resetPositionLocked() {
        if (this.mDisplayViewport == null) {
            return;
        }
        setPositionLocked(r0.logicalFrame.width() / 2.0f, this.mDisplayViewport.logicalFrame.height() / 2.0f, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeLaserWindow() {
        Slog.w(TAG, "Request remove window, mLaserView is " + (this.mLaserView == null ? "null" : "not null"));
        LaserView laserView = this.mLaserView;
        if (laserView == null) {
            return;
        }
        this.mWindowManger.removeViewImmediate(laserView);
        this.mLayoutParams = null;
        this.mLaserView = null;
        synchronized (this.mLock) {
            this.mLaserState.mVisible = false;
        }
    }

    private void addLaserWindow() {
        Slog.w(TAG, "Request add window, mLaserView is " + (this.mLaserView == null ? "null" : "not null"));
        if (this.mLaserView != null) {
            return;
        }
        LaserView laserView = new LaserView(this.mContext);
        this.mLaserView = laserView;
        laserView.setForceDarkAllowed(false);
        WindowManager.LayoutParams layoutParams = getLayoutParams();
        this.mLayoutParams = layoutParams;
        this.mWindowManger.addView(this.mLaserView, layoutParams);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshLaserState() {
        synchronized (this.mLock) {
            this.mTempLaserState.copyFrom(this.mLaserState);
            this.mLaserState.resetDirty();
        }
        if ((this.mTempLaserState.mDirty & 8) != 0 && this.mTempLaserState.mVisible) {
            LaserView laserView = this.mLaserView;
            if (laserView == null) {
                addLaserWindow();
            } else {
                laserView.setVisible(true);
            }
            this.mMiuiInputManagerInternal.hideMouseCursor();
            this.mTempLaserState.markAllDirty();
        }
        if (this.mLaserView == null) {
            return;
        }
        if ((this.mTempLaserState.mDirty & 8) != 0 && !this.mTempLaserState.mVisible) {
            this.mLaserView.setVisible(false);
            this.mLaserView.setKeepPath(false);
            this.mLaserView.clearPath();
            resetWindowRemoveTimeout();
        }
        if (!this.mTempLaserState.mVisible) {
            return;
        }
        if ((this.mTempLaserState.mDirty & 16) != 0) {
            this.mLaserView.setPresentationVisible(this.mTempLaserState.mPresentationVisible);
        }
        if ((this.mTempLaserState.mDirty & 1) != 0) {
            this.mLaserView.setPosition(this.mTempLaserState.mPosition);
        }
        if ((this.mTempLaserState.mDirty & 2) != 0) {
            this.mLaserView.setMode(this.mTempLaserState.mCurrentMode);
        }
        if ((this.mTempLaserState.mDirty & 4) != 0) {
            this.mLaserView.setKeepPath(this.mTempLaserState.mKeepPath);
        }
        pokeUserActivity();
        resetWindowRemoveTimeout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearPath() {
        LaserView laserView = this.mLaserView;
        if (laserView == null) {
            return;
        }
        laserView.clearPath();
        resetWindowRemoveTimeout();
    }

    private void resetWindowRemoveTimeout() {
        this.mHandler.removeMessages(2);
        LaserView laserView = this.mLaserView;
        if (laserView == null || laserView.needExist()) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(2, 120000L);
    }

    private void pokeUserActivity() {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastPokeUserActivityTime < POKE_USER_ACTIVITY_TIMEOUT) {
            return;
        }
        this.mLastPokeUserActivityTime = now;
        this.mPowerManager.userActivity(now, 2, 0);
    }

    private void setModeLocked(int mode) {
        if (this.mLaserState.mCurrentMode == mode) {
            return;
        }
        this.mLaserState.mCurrentMode = mode;
        invalidateLocked(2);
        if (mode == 0) {
            setKeepPathLocked(false);
        }
        sendModeToBluetooth(mode);
    }

    private void sendModeToBluetooth(final int mode) {
        Slog.w(TAG, "Sync mode to bluetooth, mode = " + mode);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.laser.LaserPointerController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LaserPointerController.this.lambda$sendModeToBluetooth$0(mode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendModeToBluetooth$0(int mode) {
        Intent intent = new Intent("com.android.input.laser.LASER_STATE");
        intent.setPackage("com.xiaomi.bluetooth");
        intent.putExtra("mode", mode);
        intent.addFlags(268435456);
        this.mContext.sendBroadcast(intent);
    }

    private void setKeepPathLocked(boolean keepPath) {
        if (this.mLaserState.mKeepPath == keepPath) {
            return;
        }
        this.mLaserState.mKeepPath = keepPath;
        invalidateLocked(4);
        this.mInputCommonConfig.setLaserIsDrawing(keepPath);
        this.mInputCommonConfig.flushToNative();
        Slog.w(TAG, "set keep path, new mode = " + keepPath);
    }

    private boolean canShowLaserViewLocked() {
        return (this.mLaserState.mCurrentMode == 0 && this.mCanShowLaser) || this.mLaserState.mCurrentMode == 1;
    }

    private boolean movePointInScreenLocked(PointF pointF) {
        DisplayViewport displayViewport = this.mDisplayViewport;
        if (displayViewport == null) {
            return false;
        }
        Rect logicalFrame = displayViewport.logicalFrame;
        if (pointF.x < logicalFrame.left) {
            pointF.x = logicalFrame.left;
        }
        if (pointF.x > logicalFrame.right) {
            pointF.x = logicalFrame.right;
        }
        if (pointF.y < logicalFrame.top) {
            pointF.y = logicalFrame.top;
        }
        if (pointF.y > logicalFrame.bottom) {
            pointF.y = logicalFrame.bottom;
            return true;
        }
        return true;
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.flags = 312;
        params.layoutInDisplayCutoutMode = 1;
        params.type = 2018;
        params.format = -3;
        params.gravity = 8388659;
        params.x = 0;
        params.y = 0;
        params.width = -1;
        params.height = -1;
        params.setTrustedOverlay();
        params.setTitle("laser");
        return params;
    }

    private DisplayViewport findDisplayViewportById(List<DisplayViewport> displayViewports, int displayId) {
        for (DisplayViewport displayViewport : displayViewports) {
            if (displayViewport.displayId == displayId) {
                return displayViewport;
            }
        }
        return null;
    }

    private void invalidateLocked(int dirty) {
        boolean wasDirty = this.mLaserState.mDirty != 0;
        this.mLaserState.mDirty |= dirty;
        if (!wasDirty) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public boolean interceptLaserKey(KeyEvent event) {
        boolean isDown = event.getAction() == 0;
        if (isDown) {
            this.mIsDownFromScreenOff = !this.mIsScreenOn;
        }
        if (this.mIsDownFromScreenOff) {
            Slog.w(TAG, "Screen is off when laser key down, not response.");
            return true;
        }
        if (isDown) {
            interceptLaserKeyDown(event);
        } else {
            interceptLaserKeyUp();
        }
        return true;
    }

    private void interceptLaserKeyDown(KeyEvent event) {
        long keyDownInterval = event.getDownTime() - this.mLastDownTime;
        this.mLastDownTime = event.getDownTime();
        if (keyDownInterval >= getMultiPressTimeout()) {
            this.mPressCounter = 1;
        } else {
            this.mPressCounter++;
        }
        if (this.mPressCounter == 1) {
            Message message = this.mHandler.obtainMessage(5);
            message.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(message, getLongPressTimeoutMs());
            Message downTimeoutMessage = this.mHandler.obtainMessage(6);
            downTimeoutMessage.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(downTimeoutMessage, KEY_DOWN_TIMEOUT);
            synchronized (this.mLock) {
                this.mIsKeyDown = true;
                this.mHandleByLongPressing = false;
                this.mHandleByKeyDownTimeout = false;
            }
            return;
        }
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        if (getMaxPressCount() > 1 && this.mPressCounter == getMaxPressCount()) {
            Message message2 = this.mHandler.obtainMessage(4, this.mPressCounter, 0);
            message2.setAsynchronous(true);
            message2.sendToTarget();
        }
    }

    private void interceptLaserKeyUp() {
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(6);
        synchronized (this.mLock) {
            this.mIsKeyDown = false;
            if (this.mHandleByKeyDownTimeout) {
                Slog.i(TAG, "Key up handle by key down timeout, not process");
                return;
            }
            if (!this.mHandleByLongPressing) {
                if (getMaxPressCount() == 1) {
                    Message msg = this.mHandler.obtainMessage(4, 1, 0);
                    msg.setAsynchronous(true);
                    msg.sendToTarget();
                } else if (this.mPressCounter < getMaxPressCount()) {
                    Message message = this.mHandler.obtainMessage(4, this.mPressCounter, 0);
                    message.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(message, getMultiPressTimeout());
                }
            } else if (this.mPressCounter == 1) {
                hideLaserOrStopDrawingLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLaserKeyLongPressed() {
        synchronized (this.mLock) {
            if (this.mIsKeyDown) {
                Slog.i(TAG, "On laser key long pressed");
                this.mCanShowLaser = true;
                this.mHandleByLongPressing = true;
                if (this.mLaserState.mCurrentMode != 0) {
                    if (this.mLaserState.mCurrentMode == 1) {
                        setKeepPathLocked(true);
                        StylusOneTrackHelper.getInstance(this.mContext).trackStylusHighLightTrigger();
                    }
                } else {
                    unfadePresentationLocked(0);
                    StylusOneTrackHelper.getInstance(this.mContext).trackStylusLaserTrigger();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLaserKeyPressed(int count) {
        Slog.i(TAG, "On laser key pressed, count = " + count);
        synchronized (this.mLock) {
            try {
                if (count == 1) {
                    if (this.mLaserState.mCurrentMode != 0) {
                        if (this.mLaserState.mCurrentMode == 1) {
                            setModeLocked(0);
                            fadePresentationLocked(0);
                        }
                    } else {
                        setModeLocked(1);
                        unfadePresentationLocked(0);
                        StylusOneTrackHelper.getInstance(this.mContext).trackStylusPenTrigger();
                    }
                } else if (count == 2) {
                    clearPath();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void hideLaserOrStopDrawingLocked() {
        if (this.mLaserState.mCurrentMode == 0) {
            fadePresentationLocked(0);
        } else if (this.mLaserState.mCurrentMode == 1) {
            setKeepPathLocked(false);
        }
        this.mCanShowLaser = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onKeyDownTimeout() {
        Slog.w(TAG, "May be the hardware occurred an exception, laser key not up");
        synchronized (this.mLock) {
            this.mHandleByKeyDownTimeout = true;
            hideLaserOrStopDrawingLocked();
        }
    }

    private long getLongPressTimeoutMs() {
        return LONG_PRESS_TIMEOUT;
    }

    private long getMultiPressTimeout() {
        return 300L;
    }

    private int getMaxPressCount() {
        return 2;
    }

    public void setScreenState(boolean isScreenOn) {
        if (this.mIsScreenOn == isScreenOn) {
            return;
        }
        this.mIsScreenOn = isScreenOn;
        if (isScreenOn) {
            return;
        }
        Slog.w(TAG, "Screen off, fade laser and reset position");
        fade(0);
        resetPosition();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LaserState {
        int mDirty = 0;
        final PointF mPosition = new PointF();
        int mCurrentMode = 0;
        boolean mKeepPath = false;
        boolean mVisible = false;
        boolean mPresentationVisible = false;

        LaserState() {
        }

        void copyFrom(LaserState laserState) {
            this.mDirty = laserState.mDirty;
            this.mPosition.set(laserState.mPosition.x, laserState.mPosition.y);
            this.mCurrentMode = laserState.mCurrentMode;
            this.mKeepPath = laserState.mKeepPath;
            this.mVisible = laserState.mVisible;
            this.mPresentationVisible = laserState.mPresentationVisible;
        }

        void resetDirty() {
            this.mDirty = 0;
        }

        void markAllDirty() {
            int i = this.mDirty | 1;
            this.mDirty = i;
            int i2 = i | 2;
            this.mDirty = i2;
            int i3 = i2 | 4;
            this.mDirty = i3;
            int i4 = i3 | 8;
            this.mDirty = i4;
            this.mDirty = i4 | 16;
        }
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        private static final int MSG_CLEAR_PATH = 3;
        private static final int MSG_KEY_DOWN_TIMEOUT = 6;
        private static final int MSG_KEY_LONG_PRESSED = 5;
        private static final int MSG_KEY_PRESSED = 4;
        private static final int MSG_REMOVE_LASER_WINDOW = 2;
        private static final int MSG_UPDATE_LASER_STATE = 1;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LaserPointerController.this.refreshLaserState();
                    return;
                case 2:
                    LaserPointerController.this.removeLaserWindow();
                    return;
                case 3:
                    LaserPointerController.this.clearPath();
                    return;
                case 4:
                    LaserPointerController.this.onLaserKeyPressed(msg.arg1);
                    return;
                case 5:
                    LaserPointerController.this.onLaserKeyLongPressed();
                    return;
                case 6:
                    LaserPointerController.this.onKeyDownTimeout();
                    return;
                default:
                    return;
            }
        }
    }
}
