package com.miui.server.input.util;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.HandlerThread;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;

/* loaded from: classes.dex */
public class MiuiInputShellCommand {
    private static final boolean DEBUG = false;
    private static final int MODE_INJECT = 3;
    private static final String TAG = MiuiInputShellCommand.class.getSimpleName();
    private static volatile MiuiInputShellCommand sInstance;
    private final Context mContext;
    private DisplayInfo mDisplayInfo;
    private final DisplayManager.DisplayListener mDisplayListener;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private final MiuiInputManagerInternal mMiuiInputManagerInternal;
    private final MotionEventGenerator mMotionEventGenerator;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.miui.server.input.util.MiuiInputShellCommand$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements DisplayManager.DisplayListener {
        AnonymousClass1() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId != 0) {
                return;
            }
            MiuiInputShellCommand miuiInputShellCommand = MiuiInputShellCommand.this;
            miuiInputShellCommand.mDisplayInfo = miuiInputShellCommand.mDisplayManagerInternal.getDisplayInfo(0);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }
    }

    private MiuiInputShellCommand() {
        AnonymousClass1 anonymousClass1 = new DisplayManager.DisplayListener() { // from class: com.miui.server.input.util.MiuiInputShellCommand.1
            AnonymousClass1() {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (displayId != 0) {
                    return;
                }
                MiuiInputShellCommand miuiInputShellCommand = MiuiInputShellCommand.this;
                miuiInputShellCommand.mDisplayInfo = miuiInputShellCommand.mDisplayManagerInternal.getDisplayInfo(0);
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }
        };
        this.mDisplayListener = anonymousClass1;
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mContext = systemContext;
        HandlerThread handlerThread = new HandlerThread("input_inject");
        handlerThread.start();
        this.mMotionEventGenerator = new MotionEventGenerator(handlerThread.getLooper());
        DisplayManager displayManager = (DisplayManager) systemContext.getSystemService(DisplayManager.class);
        displayManager.registerDisplayListener(anonymousClass1, null);
        DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mDisplayInfo = displayManagerInternal.getDisplayInfo(0);
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
    }

    public static MiuiInputShellCommand getInstance() {
        if (sInstance == null) {
            synchronized (MiuiInputShellCommand.class) {
                if (sInstance == null) {
                    sInstance = new MiuiInputShellCommand();
                }
            }
        }
        return sInstance;
    }

    public boolean swipeGenerator(int downX, int downY, int upX, int upY, int duration) {
        return this.mMotionEventGenerator.generateSwipeMotionEvent(downX, downY, upX, upY, duration, new MiuiInputShellCommand$$ExternalSyntheticLambda0(this));
    }

    public boolean swipeGenerator(int downX, int downY, int upX, int upY, int duration, int everyDelayTime) {
        return this.mMotionEventGenerator.generateSwipeMotionEvent(downX, downY, upX, upY, duration, everyDelayTime, new MiuiInputShellCommand$$ExternalSyntheticLambda0(this));
    }

    public boolean tapGenerator(int tapX, int tapY) {
        return this.mMotionEventGenerator.generateTapMotionEvent(tapX, tapY, new MiuiInputShellCommand$$ExternalSyntheticLambda0(this));
    }

    public boolean doubleTapGenerator(int tapX, int tapY, int duration) {
        return this.mMotionEventGenerator.generateDoubleTapMotionEvent(tapX, tapY, duration, new MiuiInputShellCommand$$ExternalSyntheticLambda0(this));
    }

    private void transformMotionEventForInjection(MotionEvent motionEvent) {
        int rotation = this.mDisplayInfo.rotation;
        int width = this.mDisplayInfo.logicalWidth;
        int height = this.mDisplayInfo.logicalHeight;
        if (rotation == 1) {
            motionEvent.applyTransform(MotionEvent.createRotateMatrix(3, height, width));
        } else if (rotation == 2) {
            motionEvent.applyTransform(MotionEvent.createRotateMatrix(2, width, height));
        } else if (rotation == 3) {
            motionEvent.applyTransform(MotionEvent.createRotateMatrix(1, height, width));
        }
    }

    public void injectEvent(MotionEvent motionEvent) {
        transformMotionEventForInjection(motionEvent);
        this.mMiuiInputManagerInternal.injectMotionEvent(motionEvent, 3);
    }
}
