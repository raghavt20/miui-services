package com.android.server.wm;

import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Slog;

/* loaded from: classes.dex */
public final class MiuiWindowStateEx implements IMiuiWindowStateEx {
    private static final String TAG = "MiuiWindowStateEx";
    private boolean mDimAssist;
    private boolean mIsDimWindow;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mScreenWakeLock;
    final WindowManagerService mService;
    private final WindowState mWinState;

    public MiuiWindowStateEx(WindowManagerService service, Object w) {
        this.mService = service;
        this.mWinState = (WindowState) w;
        PowerManager powerManager = service.mPowerManager;
        this.mPowerManager = powerManager;
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(805306378, "WAKEUP-FROM-CLOUD-DIM");
        this.mScreenWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
    }

    public boolean isCloudDimWindowingMode() {
        if (this.mWinState.getWindowingMode() != 5 && this.mWinState.getWindowingMode() != 3 && this.mWinState.getWindowingMode() != 4) {
            return true;
        }
        return false;
    }

    public boolean isKeepScreenOnFlag() {
        int attrFlags = this.mWinState.mAttrs.flags;
        if ((attrFlags & 128) != 0) {
            return true;
        }
        return false;
    }

    public long getScreenOffTime() {
        return this.mService.getScreenOffTimeout();
    }

    public void setIsDimWindow(boolean mIsDimWindow) {
        this.mIsDimWindow = mIsDimWindow;
    }

    public void setDimAssist(boolean mDimAssist) {
        this.mDimAssist = mDimAssist;
    }

    public void setMiuiUserActivityTimeOut(int miuiUserActivityTimeOut) {
        this.mWinState.mAttrs.userActivityTimeout = miuiUserActivityTimeOut;
    }

    public boolean isDimWindow() {
        return this.mIsDimWindow;
    }

    public boolean isAssistDim() {
        return this.mDimAssist;
    }

    public void setVsyncRate(int rate) {
        try {
            this.mWinState.mClient.notifySetVsyncRate(rate);
        } catch (RemoteException e) {
            Slog.e(TAG, "MiuiWindowStateEx.setVsyncRate catch RemoteException" + e);
        }
    }

    public void resetVsyncRate() {
        try {
            this.mWinState.mClient.notifyResetVsyncRate();
        } catch (RemoteException e) {
            Slog.e(TAG, "MiuiWindowStateEx.resetVsyncRate catch RemoteException" + e);
        }
    }
}
