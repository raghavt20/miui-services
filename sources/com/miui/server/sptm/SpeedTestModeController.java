package com.miui.server.sptm;

import android.util.Slog;

/* loaded from: classes.dex */
public class SpeedTestModeController {
    private OnSpeedTestModeChangeListener mListener;
    SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private boolean mIsInSpeedTestMode = false;

    /* loaded from: classes.dex */
    interface OnSpeedTestModeChangeListener {
        void onSpeedTestModeChange(boolean z);
    }

    public SpeedTestModeController(OnSpeedTestModeChangeListener listener) {
        this.mListener = listener;
    }

    public void setSpeedTestMode(boolean isEnable) {
        OnSpeedTestModeChangeListener onSpeedTestModeChangeListener;
        if (this.mIsInSpeedTestMode != isEnable && (onSpeedTestModeChangeListener = this.mListener) != null) {
            this.mIsInSpeedTestMode = isEnable;
            onSpeedTestModeChangeListener.onSpeedTestModeChange(isEnable);
            if (SpeedTestModeServiceImpl.DEBUG) {
                Slog.e(SpeedTestModeServiceImpl.TAG, "handleUpdateSpeedTestMode: enter spt mode = " + this.mIsInSpeedTestMode);
            }
        }
    }

    public boolean isInSpeedTestMode() {
        return this.mIsInSpeedTestMode;
    }
}
