package com.android.server.input;

import android.view.MotionEvent;
import java.io.FileDescriptor;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NativeMiuiInputManagerService {
    private final long mPtr;

    private native long init(MiuiInputManagerService miuiInputManagerService);

    public native String dump();

    public native boolean hideCursor();

    public native void hideMouseCursor();

    public native void injectMotionEvent(MotionEvent motionEvent, int i);

    public native void notifyPhotoHandleConnectionStatus(boolean z, int i);

    public native void requestRedirect(int i, int i2);

    public native boolean setCursorPosition(float f, float f2);

    public native void setDeviceShareListener(int i, FileDescriptor fileDescriptor, int i2);

    public native void setDimState(boolean z);

    public native void setInputConfig(int i, long j);

    public native void setInteractive(boolean z);

    public native void setTouchpadButtonState(int i, boolean z);

    static {
        System.loadLibrary("miinputflinger");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NativeMiuiInputManagerService(MiuiInputManagerService miuiInputManagerService) {
        this.mPtr = init(miuiInputManagerService);
    }
}
