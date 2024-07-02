package com.android.server.audio;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MediaFocusControlStubImpl implements MediaFocusControlStub {
    private static final long CHECK_DELAY_MS = 200;
    private static final int DELAY_COUNT_MAX = 8;
    private static final long DELAY_DISPATCH_FOCUS_CHANGE_MS = 400;
    private static final int MSG_L_DELAYED_FOCUS_CHANGE = 3;
    private static final String TAG = "MediaFocusControlStubImpl";
    private int mLastReleasedUid;
    private boolean mInDelayDispatch = false;
    private boolean mBtAudioSuspended = false;
    private int mAudioMode = 0;
    private int mPendingAudioMode = 0;
    private int mDelayCount = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MediaFocusControlStubImpl> {

        /* compiled from: MediaFocusControlStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MediaFocusControlStubImpl INSTANCE = new MediaFocusControlStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MediaFocusControlStubImpl m810provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MediaFocusControlStubImpl m809provideNewInstance() {
            return new MediaFocusControlStubImpl();
        }
    }

    public boolean isDelayedDispatchFocusChangeNeeded(int topUid, int focusGain) {
        Log.d(TAG, "isDelayedDispatchFocusChangeNeeded: topUid=" + topUid + ", mLastReleasedUid=" + this.mLastReleasedUid + ", mPendingAudioMode=" + this.mPendingAudioMode + ", mBtAudioSuspended=" + this.mBtAudioSuspended + ", focusGain=" + focusGain + ", mAudioMode=" + this.mAudioMode);
        if (this.mAudioMode == 3 && this.mBtAudioSuspended) {
            return true;
        }
        return inModeChanging() && focusGain == 1 && topUid != this.mLastReleasedUid;
    }

    public void setAudioMode(int mode) {
        Log.d(TAG, "setAudioMode() " + mode);
        this.mAudioMode = mode;
    }

    public void setPendingAudioMode(int mode) {
        this.mPendingAudioMode = mode;
    }

    private boolean inModeChanging() {
        int i = this.mPendingAudioMode;
        boolean fromCommunicationToNormal = i == 3 && this.mAudioMode == 0;
        boolean fromNormalToCommunication = i == 0 && this.mAudioMode == 3;
        Log.d(TAG, "inModeChanging: fromCommunicationToNormal=" + fromCommunicationToNormal + ", fromNormalToCommunication=" + fromNormalToCommunication);
        return fromCommunicationToNormal || fromNormalToCommunication;
    }

    public void setBtAudioSuspended(boolean btAudioSuspended) {
        Log.d(TAG, "setBtAudioSuspended() " + btAudioSuspended);
        this.mBtAudioSuspended = btAudioSuspended;
    }

    public void setAudioFocusDelayDispatchState(boolean state) {
        Log.d(TAG, "setAudioFocusDelayDispatchState(): prior state=" + this.mInDelayDispatch + ", current state=" + state);
        this.mInDelayDispatch = state;
    }

    public void setLastReleasedUid(int uid) {
        this.mLastReleasedUid = uid;
    }

    public void delayFocusChangeDispatch(int focusGain, FocusRequester fr, String clientId, Handler focusHandler) {
        Log.d(TAG, "delay dispatching " + focusGain + " to " + clientId + " for " + DELAY_DISPATCH_FOCUS_CHANGE_MS + "ms");
        postDelayedFocusChange(focusGain, fr, DELAY_DISPATCH_FOCUS_CHANGE_MS, focusHandler);
        setAudioFocusDelayDispatchState(true);
    }

    public void postDelayedFocusChange(int focusChange, FocusRequester fr, long delayMs, Handler focusHandler) {
        Log.d(TAG, "postDelayedFocusChange(): send focusChange=" + focusChange + " to " + fr.getPackageName() + " delayMs=" + delayMs);
        focusHandler.removeEqualMessages(3, fr);
        focusHandler.sendMessageDelayed(focusHandler.obtainMessage(3, focusChange, 0, fr), delayMs);
    }

    public void handleDelayedMessageWithdraw(FocusRequester fr, Handler focusHandler) {
        if (this.mInDelayDispatch) {
            Log.d(TAG, "handleDelayedMessageWithdraw(): withdraw message to " + fr.getPackageName());
            focusHandler.removeEqualMessages(3, fr);
            setAudioFocusDelayDispatchState(false);
        }
    }

    public void handleDelayedMessage(Message msg, Handler focusHandler) {
        FocusRequester fr = (FocusRequester) msg.obj;
        int focusChange = msg.arg1;
        if ((this.mBtAudioSuspended || this.mAudioMode != 0 || inModeChanging()) && this.mDelayCount < 8) {
            Log.d(TAG, "bt/audioMode switch not ready, delay dispatching focus change 200ms, count " + this.mDelayCount);
            this.mDelayCount++;
            postDelayedFocusChange(focusChange, fr, CHECK_DELAY_MS, focusHandler);
        } else {
            Log.d(TAG, "handling MSG_L_DELAYED_FOCUS_CHANGE focusChange=" + focusChange + " packageName=" + fr.getPackageName() + " clientId=" + fr.getClientId());
            fr.dispatchFocusChange(focusChange);
            setAudioFocusDelayDispatchState(false);
            this.mDelayCount = 0;
        }
    }
}
