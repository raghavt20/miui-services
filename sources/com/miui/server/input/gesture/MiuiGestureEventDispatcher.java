package com.miui.server.input.gesture;

import android.os.Looper;
import android.util.Slog;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class MiuiGestureEventDispatcher extends InputEventReceiver {
    private static final String TAG = "MiuiGestureEventDispatcher";
    private final InputChannel mInputChannel;
    private final ArrayList<MiuiGestureListener> mListeners;
    private MiuiGestureListener[] mListenersArray;

    public MiuiGestureEventDispatcher(InputChannel inputChannel, Looper looper) {
        super(inputChannel, looper);
        this.mListeners = new ArrayList<>();
        this.mListenersArray = new MiuiGestureListener[0];
        this.mInputChannel = inputChannel;
    }

    public void onInputEvent(InputEvent event) {
        MiuiGestureListener[] listeners;
        if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
            MotionEvent motionEvent = (MotionEvent) event;
            synchronized (this.mListeners) {
                if (this.mListenersArray == null) {
                    MiuiGestureListener[] miuiGestureListenerArr = new MiuiGestureListener[this.mListeners.size()];
                    this.mListenersArray = miuiGestureListenerArr;
                    this.mListeners.toArray(miuiGestureListenerArr);
                }
                listeners = this.mListenersArray;
            }
            for (MiuiGestureListener miuiGestureListener : listeners) {
                miuiGestureListener.onPointerEvent(motionEvent);
            }
        }
        finishInputEvent(event, false);
    }

    public void registerInputEventListener(MiuiGestureListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                Slog.i(TAG, "registerInputEventListener trying to register" + listener + " twice. ");
            } else {
                this.mListeners.add(listener);
                this.mListenersArray = null;
            }
        }
    }

    public void unregisterInputEventListener(MiuiGestureListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                Slog.i(TAG, "unregisterInputEventListener" + listener + "not registered.");
            } else {
                this.mListeners.remove(listener);
                this.mListenersArray = null;
            }
        }
    }

    public int getGestureListenerCount() {
        int size;
        synchronized (this.mListeners) {
            size = this.mListeners.size();
        }
        return size;
    }

    public void dispose() {
        super.dispose();
        this.mInputChannel.dispose();
        synchronized (this.mListeners) {
            this.mListeners.clear();
            this.mListenersArray = null;
        }
    }
}
