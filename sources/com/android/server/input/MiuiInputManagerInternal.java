package com.android.server.input;

import android.hardware.display.DisplayViewport;
import android.view.MotionEvent;
import android.view.PointerIcon;
import com.miui.server.input.stylus.laser.PointerControllerInterface;
import java.io.FileDescriptor;
import java.util.List;

/* loaded from: classes.dex */
public abstract class MiuiInputManagerInternal {
    public abstract boolean doubleTap(int i, int i2, int i3);

    public abstract List<DisplayViewport> getCurrentDisplayViewPorts();

    public abstract int getCurrentPointerDisplayId();

    public abstract void hideMouseCursor();

    public abstract void injectMotionEvent(MotionEvent motionEvent, int i);

    public abstract void notifyPhotoHandleConnectionStatus(boolean z, int i);

    public abstract PointerControllerInterface obtainLaserPointerController();

    public abstract void setCustomPointerIcon(PointerIcon pointerIcon);

    public abstract void setDeviceShareListener(int i, FileDescriptor fileDescriptor, int i2);

    public abstract void setDimState(boolean z);

    public abstract void setInputConfig(int i, long j);

    public abstract void setPointerIconType(int i);

    public abstract void setScreenState(boolean z);

    public abstract void setTouchpadButtonState(int i, boolean z);

    public abstract boolean swipe(int i, int i2, int i3, int i4, int i5);

    public abstract boolean swipe(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract boolean tap(int i, int i2);
}
