package com.miui.server.input.util;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

/* loaded from: classes.dex */
public final class MotionEventGenerator {
    private static final int DEFAULT_DELAY_MILLIS = 5;
    private int mAllStep;
    private int mCurrentStep;
    private int mDoubleTapDuration;
    private long mDownTime;
    private float mEndX;
    private float mEndY;
    private long mEveryDelayTime;
    private GenerateResultCallback mGenerateResultCallback;
    private final Handler mHandler;
    private volatile boolean mIsRunning;
    private float mStartX;
    private float mStartY;
    private float mTapX;
    private float mTapY;
    private final Runnable mSwipeRunnable = new Runnable() { // from class: com.miui.server.input.util.MotionEventGenerator$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            MotionEventGenerator.this.sendSwipe();
        }
    };
    private final Runnable mTapRunnable = new Runnable() { // from class: com.miui.server.input.util.MotionEventGenerator$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            MotionEventGenerator.this.sendTap();
        }
    };
    private final Runnable mDoubleTapRunnable = new Runnable() { // from class: com.miui.server.input.util.MotionEventGenerator$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            MotionEventGenerator.this.sendDoubleTap();
        }
    };
    private final Object mLock = new Object();

    /* loaded from: classes.dex */
    public interface GenerateResultCallback {
        void onMotionEvent(MotionEvent motionEvent);
    }

    public MotionEventGenerator(Looper looper) {
        this.mHandler = new Handler(looper == null ? Looper.myLooper() : looper);
    }

    public boolean isGenerating() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSwipe() {
        MotionEvent motionEvent;
        int currentAction;
        int i = this.mCurrentStep;
        if (i == 0) {
            currentAction = 0;
            long now = now();
            this.mDownTime = now;
            motionEvent = MotionEvent.obtain(now, now, 0, this.mStartX, this.mStartY, 0);
        } else {
            int i2 = this.mAllStep;
            if (i >= i2) {
                currentAction = 1;
                motionEvent = MotionEvent.obtain(this.mDownTime, now(), 1, this.mEndX, this.mEndY, 0);
            } else {
                float f = this.mStartX;
                float x = f + (((this.mEndX - f) / i2) * i);
                float f2 = this.mStartY;
                float y = f2 + (((this.mEndY - f2) / i2) * i);
                motionEvent = MotionEvent.obtain(this.mDownTime, now(), 2, x, y, 0);
                currentAction = 2;
            }
        }
        motionEvent.setSource(4098);
        this.mGenerateResultCallback.onMotionEvent(motionEvent);
        if (currentAction != 1) {
            this.mCurrentStep++;
            this.mHandler.postDelayed(this.mSwipeRunnable, this.mEveryDelayTime);
        } else {
            setRunningStatus(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendTap() {
        generateTapMotionEvent();
        setRunningStatus(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendDoubleTap() {
        generateTapMotionEvent();
        this.mHandler.postDelayed(this.mTapRunnable, this.mDoubleTapDuration);
    }

    private void generateTapMotionEvent() {
        long currentTime = now();
        MotionEvent down = MotionEvent.obtain(currentTime, currentTime, 0, this.mTapX, this.mTapY, 0);
        down.setSource(4098);
        MotionEvent up = MotionEvent.obtain(currentTime, currentTime, 1, this.mTapX, this.mTapY, 0);
        up.setSource(4098);
        this.mGenerateResultCallback.onMotionEvent(down);
        this.mGenerateResultCallback.onMotionEvent(up);
    }

    public boolean generateSwipeMotionEvent(float startX, float startY, float endX, float endY, int duration, GenerateResultCallback callback) {
        return generateSwipeMotionEvent(startX, startY, endX, endY, duration, 5L, callback);
    }

    public boolean generateSwipeMotionEvent(float startX, float startY, float endX, float endY, int duration, long everyDelayTime, GenerateResultCallback callback) {
        int step;
        if (getRunningStatus() || duration < 0 || everyDelayTime < 0 || (step = (int) (duration / everyDelayTime)) < 1) {
            return false;
        }
        setRunningStatus(true);
        this.mStartX = startX;
        this.mStartY = startY;
        this.mEndX = endX;
        this.mEndY = endY;
        this.mAllStep = step;
        this.mCurrentStep = 0;
        this.mEveryDelayTime = everyDelayTime;
        this.mGenerateResultCallback = callback;
        this.mDownTime = -1L;
        this.mHandler.post(this.mSwipeRunnable);
        return true;
    }

    public boolean generateTapMotionEvent(float x, float y, GenerateResultCallback callback) {
        if (getRunningStatus()) {
            return false;
        }
        setRunningStatus(true);
        this.mTapX = x;
        this.mTapY = y;
        this.mGenerateResultCallback = callback;
        this.mHandler.post(this.mTapRunnable);
        return true;
    }

    public boolean generateDoubleTapMotionEvent(float x, float y, int duration, GenerateResultCallback callback) {
        if (getRunningStatus() || duration < 0) {
            return false;
        }
        setRunningStatus(true);
        this.mTapX = x;
        this.mTapY = y;
        this.mDoubleTapDuration = duration;
        this.mGenerateResultCallback = callback;
        this.mHandler.post(this.mDoubleTapRunnable);
        return true;
    }

    private void setRunningStatus(boolean running) {
        synchronized (this.mLock) {
            this.mIsRunning = running;
        }
    }

    private boolean getRunningStatus() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsRunning;
        }
        return z;
    }

    private static long now() {
        return SystemClock.uptimeMillis();
    }
}
