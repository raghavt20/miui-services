package com.android.server.tof;

import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.miui.server.stability.DumpSysInfoUtil;

/* loaded from: classes.dex */
public class InputHelper {
    private static final int SWIPE_DURATION = 150;
    private static final String TAG = "TofInputHelper";
    private static final String THREAD_NAME = "input.tof";
    private Handler mHandler;
    private MiuiInputManagerInternal mMiuiInputManagerInternal;
    private WindowManagerService mWindowManagerService;
    private Point mUpPoint = new Point();
    private Point mDownPoint = new Point();
    private Point mLeftPoint = new Point();
    private Point mRightPoint = new Point();

    public InputHelper() {
        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();
        this.mHandler = new Handler(thread.getLooper());
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        this.mWindowManagerService = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        updateSwipePoint();
        registerDisplayChangeListener();
    }

    public void injectSupportInputEvent(int feature) {
        switch (feature) {
            case 1:
                sendMediaPlayPauseEvent();
                return;
            case 2:
                sendDpadRightEvent();
                return;
            case 4:
                sendDpadLeftEvent();
                return;
            case 8:
                sendMediaNextEvent();
                return;
            case 16:
                sendMediaPreviousEvent();
                return;
            case 32:
                sendPageDownEvent();
                return;
            case 64:
                sendPageUpEvent();
                return;
            case 128:
                sendCallEvent();
                return;
            case 256:
                sendEndCallEvent();
                return;
            case 512:
                sendVolumeUpEvent();
                return;
            case 1024:
                sendVolumeDownEvent();
                return;
            case 2048:
                sendSwipeUpEvent();
                return;
            case 4096:
                sendSwipeDownEvent();
                return;
            case 8192:
                sendMediaFastForward();
                return;
            case 16384:
                sendMediaRewind();
                return;
            case 32768:
                sendMediaPlayPauseSpaceEvent();
                return;
            case 65536:
                sendSwipeLeftEvent();
                return;
            case 131072:
                sendSwipeRightEvent();
                return;
            default:
                Slog.e(TAG, "unsupported feature:" + feature);
                return;
        }
    }

    public void sendEndCallEvent() {
        injectKeyEvent(6);
    }

    public void sendCallEvent() {
        injectKeyEvent(5);
    }

    public void sendPageUpEvent() {
        injectKeyEvent(92);
    }

    public void sendPageDownEvent() {
        injectKeyEvent(93);
    }

    public void sendMediaNextEvent() {
        injectKeyEvent(87);
    }

    public void sendMediaPreviousEvent() {
        injectKeyEvent(88);
    }

    public void sendDpadLeftEvent() {
        injectKeyEvent(21);
    }

    public void sendDpadRightEvent() {
        injectKeyEvent(22);
    }

    public void sendMediaRewind() {
        injectKeyEvent(89);
    }

    public void sendMediaFastForward() {
        injectKeyEvent(90);
    }

    public void sendMediaPlayPauseEvent() {
        injectKeyEvent(85);
    }

    public void sendMediaPlayPauseSpaceEvent() {
        injectKeyEvent(62);
    }

    public void sendVolumeUpEvent() {
        injectKeyEvent(24);
    }

    public void sendVolumeDownEvent() {
        injectKeyEvent(25);
    }

    public void sendSwipeUpEvent() {
        simulatedSwipe(this.mDownPoint, this.mUpPoint);
    }

    public void sendSwipeDownEvent() {
        simulatedSwipe(this.mUpPoint, this.mDownPoint);
    }

    public void sendSwipeLeftEvent() {
        simulatedSwipe(this.mRightPoint, this.mLeftPoint);
    }

    public void sendSwipeRightEvent() {
        simulatedSwipe(this.mLeftPoint, this.mRightPoint);
    }

    private void injectKeyEvent(final int keyCode) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.InputHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InputHelper.this.lambda$injectKeyEvent$0(keyCode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$injectKeyEvent$0(int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = KeyEvent.obtain(now, now, 0, keyCode, 0, 0, -1, 0, 0, 0, 0, null);
        try {
            boolean result = injectKeyEvent(event);
            Slog.i(TAG, "injectKeyEvent result:" + (result & injectKeyEvent(KeyEvent.changeAction(event, 1))) + "," + KeyEvent.keyCodeToString(keyCode));
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, e.toString());
        }
    }

    private boolean injectKeyEvent(KeyEvent event) {
        return InputManager.getInstance().injectInputEvent(event, 1);
    }

    public void simulatedSwipe(final Point start, final Point end) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.InputHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InputHelper.this.lambda$simulatedSwipe$1(start, end);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$simulatedSwipe$1(Point start, Point end) {
        this.mMiuiInputManagerInternal.swipe(start.x, start.y, end.x, end.y, 150);
    }

    private void registerDisplayChangeListener() {
        DisplayManagerGlobal mGlobal = DisplayManagerGlobal.getInstance();
        mGlobal.registerDisplayListener(new DisplayManager.DisplayListener() { // from class: com.android.server.tof.InputHelper.1
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                InputHelper.this.updateSwipePoint();
            }
        }, this.mHandler, 4L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSwipePoint() {
        int displayWidth;
        int displayHeight;
        Point point = new Point();
        this.mWindowManagerService.getBaseDisplaySize(0, point);
        int rotation = this.mWindowManagerService.getDefaultDisplayRotation();
        if (rotation == 1 || rotation == 3) {
            displayWidth = point.y;
            displayHeight = point.x;
        } else {
            displayWidth = point.x;
            displayHeight = point.y;
        }
        this.mUpPoint.set(displayWidth / 3, displayHeight / 3);
        this.mDownPoint.set((displayWidth / 3) + 50, (displayHeight * 2) / 3);
        this.mLeftPoint.set(displayWidth / 4, displayHeight / 5);
        this.mRightPoint.set((displayWidth * 3) / 4, (displayHeight / 5) + 50);
    }
}
