package com.android.server.sensors;

import android.R;
import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.os.HandlerExecutor;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.miui.base.MiuiStubRegistry;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class SensorServiceImpl extends SensorServiceStub {
    private static final String TAG = "SensorServiceImpl";
    private Context mContext;
    private SensorService mSensorService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SensorServiceImpl> {

        /* compiled from: SensorServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SensorServiceImpl INSTANCE = new SensorServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SensorServiceImpl m2317provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SensorServiceImpl m2316provideNewInstance() {
            return new SensorServiceImpl();
        }
    }

    public void initialize(Context context, SensorService sensorService) {
        this.mContext = context;
        this.mSensorService = sensorService;
    }

    public void registerDeviceStateCallback() {
        boolean isFoldOrFlipDevice = MiuiMultiDisplayTypeInfo.isFoldDeviceInside() || MiuiMultiDisplayTypeInfo.isFlipDevice();
        Slog.i(TAG, "isFoldOrFlipDevice: " + isFoldOrFlipDevice);
        if (isFoldOrFlipDevice) {
            DeviceStateManager deviceStateManager = (DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class);
            deviceStateManager.registerCallback(new HandlerExecutor(BackgroundThread.getHandler()), new DeviceStateListener(this.mContext));
        }
    }

    /* loaded from: classes.dex */
    class DeviceStateListener implements DeviceStateManager.DeviceStateCallback {
        private final int[] mDeviceOrientSensorReversedFoldedDeviceStates;
        private final int[] mReversedFoldedDeviceStates;
        private final int REVERT_DEVICE_NO_SENSOR = 0;
        private final int REVERT_DEVICE_DEVICE_ORIENT = 1;
        private final int REVERT_DEVICE_ACCELERATION = 2;

        public DeviceStateListener(Context context) {
            this.mDeviceOrientSensorReversedFoldedDeviceStates = context.getResources().getIntArray(R.array.config_display_no_service_when_sim_unready);
            this.mReversedFoldedDeviceStates = context.getResources().getIntArray(R.array.vendor_disallowed_apps_managed_profile);
        }

        public void onStateChanged(int deviceState) {
            Slog.i(SensorServiceImpl.TAG, "deviceState: " + deviceState);
            int reversedFlag = 0 | (ArrayUtils.contains(this.mDeviceOrientSensorReversedFoldedDeviceStates, deviceState) ? 1 : 0);
            SensorServiceImpl.this.mSensorService.callReversedStateNative(reversedFlag | (ArrayUtils.contains(this.mReversedFoldedDeviceStates, deviceState) ? 2 : 0));
            SensorServiceImpl.this.mSensorService.callDeviceStateNative(deviceState);
        }
    }
}
