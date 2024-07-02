package com.android.server.audio;

import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Intent;
import android.media.AudioSystem;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.Objects;

/* loaded from: classes.dex */
public class BtHelperImpl implements BtHelperStub {
    private static final int AUDIO_FORMAT_FORCE_AOSP = 2130706432;
    private static final int SPATIAL_AUDIO_TYPE_SUPPORT_GYRO_AND_ALGO = 3;
    private static final int SPATIAL_AUDIO_TYPE_SUPPORT_GYRO_ONLY = 2;
    private static final String TAG = "AS.BtHelperImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BtHelperImpl> {

        /* compiled from: BtHelperImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BtHelperImpl INSTANCE = new BtHelperImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BtHelperImpl m776provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BtHelperImpl m775provideNewInstance() {
            return new BtHelperImpl();
        }
    }

    public boolean checkEncoderFormat(BluetoothCodecConfig btCodecConfig) {
        if (getFormat(btCodecConfig) == AUDIO_FORMAT_FORCE_AOSP) {
            return true;
        }
        return false;
    }

    public int getFormat(BluetoothCodecConfig config) {
        try {
            Method isGetFormatMethod = BluetoothCodecConfig.class.getDeclaredMethod("getEncoderFormat", null);
            isGetFormatMethod.setAccessible(true);
            return ((Integer) isGetFormatMethod.invoke(config, null)).intValue();
        } catch (Exception e) {
            Log.e(TAG, "getFormat error " + e);
            return 0;
        }
    }

    public boolean handleSpatialAudioDeviceConnect(BluetoothDevice spatialDevice, BluetoothDevice currActiveDevice, int spatialAudioType) {
        Log.i(TAG, "spatialAudioType: " + spatialAudioType);
        if (!Objects.equals(spatialDevice, currActiveDevice)) {
            return false;
        }
        if (spatialAudioType == 2 || spatialAudioType == 3) {
            Log.d(TAG, "spatial audio device connect");
            AudioSystem.setParameters("spatial_audio_headphone_connect=true");
            return true;
        }
        return false;
    }

    public void handleSpatialAudioDeviceDisConnect(BluetoothDevice spatialDevice, BluetoothDevice previousActiveDevice) {
        if (spatialDevice != null && Objects.equals(spatialDevice, previousActiveDevice)) {
            Log.d(TAG, "change to not spatial audio device");
            AudioSystem.setParameters("spatial_audio_headphone_connect=false");
        }
    }

    public boolean disconnectBtScoHelper(BluetoothHeadset bluetoothheadset, BtHelper bthelper) {
        Log.i(TAG, "bthelper try to disconnect the sco");
        bthelper.broadcastScoConnectionState(0);
        if (bluetoothheadset == null) {
            Log.i(TAG, "mBluetoothHeadset == null");
            return false;
        }
        return bluetoothheadset.stopScoUsingVirtualVoiceCall();
    }

    public boolean connectBtScoHelper(BluetoothHeadset bluetoothheadset, BtHelper bthelper) {
        Log.i(TAG, "bthelper try to connect the sco");
        bthelper.broadcastScoConnectionState(1);
        if (bluetoothheadset == null) {
            Log.i(TAG, "mBluetoothHeadset == null");
            return false;
        }
        return bluetoothheadset.startScoUsingVirtualVoiceCall();
    }

    public boolean isFakeHfp(Intent intent) {
        String broadcastType = intent.getStringExtra("android.bluetooth.device.extra.NAME");
        return "fake_hfp_broadcast".equals(broadcastType);
    }
}
