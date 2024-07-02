package com.android.server.devicestate;

import android.R;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.Optional;
import java.util.Set;

@MiuiStubHead(manifestName = "com.android.server.devicestate.DeviceStateManagerServiceStub$$")
/* loaded from: classes.dex */
public class DeviceStateManagerServiceImpl extends DeviceStateManagerServiceStub {
    private static final String CURRENT_DEVICE_STATE = "current_device_state";
    private static final String DEVICE_POSTURE = "device_posture";
    private static final int FLAG_TENT_MODE = Integer.MIN_VALUE;
    private static final int FOLDABLE_DEVICE_STATE_CLOSED = 0;
    private static final int FOLDABLE_DEVICE_STATE_HALF_OPENED = 2;
    private static final int FOLDABLE_DEVICE_STATE_OPENED = 3;
    private static final int FOLDABLE_DEVICE_STATE_OPENED_PRESENTATION = 5;
    private static final int FOLDABLE_DEVICE_STATE_OPENED_REVERSE = 4;
    private static final int FOLDABLE_DEVICE_STATE_OPENED_REVERSE_PRESENTATION = 6;
    private static final int FOLDABLE_DEVICE_STATE_TENT = 1;
    private static final int POSTURE_CLOSED = 1;
    private static final int POSTURE_FLIPPED = 4;
    private static final int POSTURE_HALF_OPENED = 2;
    private static final int POSTURE_OPENED = 3;
    private static final int POSTURE_UNKNOWN = 0;
    public static final String TAG = "DeviceStateManagerServiceImpl";
    private static final boolean sDebug = false;
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private Context mContext;
    private Set<Integer> mFoldedDeviceStates;
    private Object mLock;
    private OverrideRequestController mOverrideRequestController;
    private WindowManagerPolicy mPolicy;
    private int[] mReversedFoldedDeviceStates;
    private DeviceStateManagerService mService;
    private int[] mStrictFoldedDeviceStates;
    private ActivityTaskManagerInternal.ScreenObserver mOverrideRequestScreenObserver = new OverrideRequestScreenObserver();
    private boolean mInteractive = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DeviceStateManagerServiceImpl> {

        /* compiled from: DeviceStateManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DeviceStateManagerServiceImpl INSTANCE = new DeviceStateManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DeviceStateManagerServiceImpl m968provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DeviceStateManagerServiceImpl m967provideNewInstance() {
            return new DeviceStateManagerServiceImpl();
        }
    }

    public void init(Context context, DeviceStateManagerService service, Object lock, OverrideRequestController overrideRequestController) {
        this.mContext = context;
        this.mOverrideRequestController = overrideRequestController;
        this.mService = service;
        this.mLock = lock;
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mReversedFoldedDeviceStates = this.mContext.getResources().getIntArray(R.array.vendor_disallowed_apps_managed_profile);
        this.mStrictFoldedDeviceStates = this.mContext.getResources().getIntArray(17236155);
    }

    public void onStart(Set<Integer> foldedDeviceStates) {
        this.mFoldedDeviceStates = foldedDeviceStates;
        this.mActivityTaskManagerInternal.registerScreenObserver(this.mOverrideRequestScreenObserver);
    }

    public void setBootPhase(int phase) {
        if (phase == 500) {
            this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
    }

    public boolean adjustConditionSatisfiedLocked(boolean conditionSatisfied, DeviceState deviceState, int lastReportedState) {
        if (!conditionSatisfied || deviceState == null) {
            return false;
        }
        if (deviceState.hasFlag(FLAG_TENT_MODE)) {
            if ((!ArrayUtils.contains(this.mStrictFoldedDeviceStates, lastReportedState) && lastReportedState != deviceState.getIdentifier()) || !this.mInteractive) {
                return false;
            }
            return conditionSatisfied;
        }
        return conditionSatisfied;
    }

    public void handleBaseStateChangedLocked(int lastBaseStateIdentifier) {
        if (this.mFoldedDeviceStates == null) {
            Slog.i(TAG, "FoldedDeviceStates is null");
            return;
        }
        Optional<DeviceState> curBaseState = this.mService.getBaseState();
        Optional<DeviceState> committedState = this.mService.getCommittedState();
        boolean isCurrentStateFolded = lastBaseStateIdentifier != -1 && this.mFoldedDeviceStates.contains(Integer.valueOf(lastBaseStateIdentifier));
        boolean isNewStateFolded = curBaseState.isPresent() && this.mFoldedDeviceStates.contains(Integer.valueOf(curBaseState.get().getIdentifier()));
        if (isCurrentStateFolded != isNewStateFolded) {
            this.mOverrideRequestController.handleFoldStateChanged();
        }
        if (curBaseState.isPresent() && committedState.isPresent()) {
            updateDevicePosture(curBaseState.get().getIdentifier(), committedState.get().getIdentifier());
        }
    }

    public void handleCommittedStateChangedLocked() {
        Optional<DeviceState> baseState = this.mService.getBaseState();
        Optional<DeviceState> committedState = this.mService.getCommittedState();
        if (!baseState.isPresent() || !committedState.isPresent()) {
            Slog.i(TAG, "skip handle committed state changed. (" + baseState.isPresent() + "," + committedState.isPresent() + ")");
            return;
        }
        int baseStateIdentifier = baseState.get().getIdentifier();
        int committedStateIdentifier = committedState.get().getIdentifier();
        updateDevicePosture(baseStateIdentifier, committedStateIdentifier);
        Settings.Global.putInt(this.mContext.getContentResolver(), CURRENT_DEVICE_STATE, committedStateIdentifier);
    }

    private void updateDevicePosture(int baseStateIdentifier, int committedStateIdentifier) {
        int devicePosture;
        if (ArrayUtils.contains(this.mFoldedDeviceStates, Integer.valueOf(committedStateIdentifier))) {
            devicePosture = 1;
        } else if (committedStateIdentifier == 2) {
            devicePosture = 2;
        } else if (committedStateIdentifier == 5 && baseStateIdentifier == 2) {
            devicePosture = 2;
        } else {
            devicePosture = 3;
        }
        int curDevicePosture = Settings.Global.getInt(this.mContext.getContentResolver(), DEVICE_POSTURE, -1);
        if (curDevicePosture != devicePosture) {
            Settings.Global.putInt(this.mContext.getContentResolver(), DEVICE_POSTURE, devicePosture);
        }
    }

    public boolean skipDueToReversedState() {
        if (this.mPolicy != null && isReversedState()) {
            cancelStateRequest();
            this.mPolicy.lockNow((Bundle) null);
            Slog.i(TAG, "Do not sleep but lock device when device state is reversed.");
            return true;
        }
        return false;
    }

    private boolean isReversedState() {
        boolean isReversedStateLocked;
        synchronized (this.mLock) {
            isReversedStateLocked = isReversedStateLocked();
        }
        return isReversedStateLocked;
    }

    private boolean isReversedStateLocked() {
        if (this.mService.getCommittedState().isPresent()) {
            int currentState = ((DeviceState) this.mService.getCommittedState().get()).getIdentifier();
            if (ArrayUtils.contains(this.mReversedFoldedDeviceStates, currentState)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void cancelStateRequest() {
        this.mService.cancelStateRequestInternal();
    }

    /* loaded from: classes.dex */
    private class OverrideRequestScreenObserver implements ActivityTaskManagerInternal.ScreenObserver {
        private OverrideRequestScreenObserver() {
        }

        public void onAwakeStateChanged(boolean isAwake) {
            DeviceStateManagerServiceImpl.this.mInteractive = isAwake;
        }

        public void onKeyguardStateChanged(boolean isShowing) {
        }
    }
}
