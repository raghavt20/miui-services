package com.android.server.wm;

import android.content.Context;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;

/* loaded from: classes.dex */
public class SchedBoostGesturesEvent implements MiuiGestureListener {
    private static final int MAX_FLING_TIME_MILLIS = 5000;
    private static final String TAG = "SchedBoostGestures";
    private Context mContext;
    private int mDisplayId;
    private GestureDetector mGestureDetector;
    private GesturesEventListener mGesturesEventListener;
    private MyHandler mHandler;
    private long mLastFlingTime;
    private boolean mScrollFired;

    /* loaded from: classes.dex */
    public interface GesturesEventListener {
        void onDown();

        void onFling(float f, float f2, int i);

        void onMove();

        void onScroll(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
        }
    }

    public SchedBoostGesturesEvent(Looper looper) {
        this.mHandler = new MyHandler(looper);
    }

    public void init(Context context) {
        this.mContext = context;
        this.mDisplayId = context.getDisplayId();
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.SchedBoostGesturesEvent$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SchedBoostGesturesEvent.this.lambda$init$0();
            }
        });
        MiuiGestureMonitor.getInstance(context).registerPointerEventListener(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$init$0() {
        int displayId = this.mDisplayId;
        DisplayInfo info = DisplayManagerGlobal.getInstance().getDisplayInfo(displayId);
        if (info == null) {
            Slog.w(TAG, "Cannot create GestureDetector, display removed:" + displayId);
        } else {
            this.mGestureDetector = new GestureDetector(this.mContext, new FlingGestureDetector(), this.mHandler) { // from class: com.android.server.wm.SchedBoostGesturesEvent.1
            };
        }
    }

    public void setGesturesEventListener(GesturesEventListener listener) {
        this.mGesturesEventListener = listener;
    }

    @Override // com.miui.server.input.gesture.MiuiGestureListener
    public void onPointerEvent(MotionEvent event) {
        final MotionEvent motionEvent = event.copy();
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.SchedBoostGesturesEvent$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SchedBoostGesturesEvent.this.lambda$onPointerEvent$1(motionEvent);
            }
        });
    }

    /* renamed from: onSyncPointerEvent, reason: merged with bridge method [inline-methods] */
    public void lambda$onPointerEvent$1(MotionEvent event) {
        GesturesEventListener gesturesEventListener;
        if (this.mGestureDetector != null && event.isTouchEvent()) {
            this.mGestureDetector.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case 0:
                this.mScrollFired = false;
                GesturesEventListener gesturesEventListener2 = this.mGesturesEventListener;
                if (gesturesEventListener2 != null) {
                    gesturesEventListener2.onDown();
                    return;
                }
                return;
            case 1:
            case 3:
                if (this.mScrollFired && (gesturesEventListener = this.mGesturesEventListener) != null) {
                    gesturesEventListener.onScroll(false);
                }
                this.mScrollFired = false;
                return;
            case 2:
                GesturesEventListener gesturesEventListener3 = this.mGesturesEventListener;
                if (gesturesEventListener3 != null) {
                    gesturesEventListener3.onMove();
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private OverScroller mOverscroller;

        FlingGestureDetector() {
            this.mOverscroller = new OverScroller(SchedBoostGesturesEvent.this.mContext);
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onSingleTapUp(MotionEvent e) {
            if (!this.mOverscroller.isFinished()) {
                this.mOverscroller.forceFinished(true);
            }
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY) {
            this.mOverscroller.computeScrollOffset();
            long now = SystemClock.uptimeMillis();
            if (SchedBoostGesturesEvent.this.mLastFlingTime != 0 && now > SchedBoostGesturesEvent.this.mLastFlingTime + 5000) {
                this.mOverscroller.forceFinished(true);
            }
            this.mOverscroller.fling(0, 0, (int) velocityX, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int duration = this.mOverscroller.getDuration();
            if (duration > 5000) {
                duration = 5000;
            }
            SchedBoostGesturesEvent.this.mLastFlingTime = now;
            if (SchedBoostGesturesEvent.this.mGesturesEventListener != null) {
                SchedBoostGesturesEvent.this.mGesturesEventListener.onFling(velocityX, velocityY, duration);
            }
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!SchedBoostGesturesEvent.this.mScrollFired) {
                if (SchedBoostGesturesEvent.this.mGesturesEventListener != null) {
                    SchedBoostGesturesEvent.this.mGesturesEventListener.onScroll(true);
                }
                SchedBoostGesturesEvent.this.mScrollFired = true;
            }
            return true;
        }
    }
}
