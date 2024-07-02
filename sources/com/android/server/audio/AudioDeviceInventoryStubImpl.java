package com.android.server.audio;

import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioDeviceInventory;
import com.miui.base.MiuiStubRegistry;
import java.util.LinkedHashMap;

/* loaded from: classes.dex */
public class AudioDeviceInventoryStubImpl implements AudioDeviceInventoryStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AudioDeviceInventoryStubImpl> {

        /* compiled from: AudioDeviceInventoryStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AudioDeviceInventoryStubImpl INSTANCE = new AudioDeviceInventoryStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AudioDeviceInventoryStubImpl m727provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AudioDeviceInventoryStubImpl m726provideNewInstance() {
            return new AudioDeviceInventoryStubImpl();
        }
    }

    public void postSetVolumeIndex(AudioDeviceBroker.BtDeviceInfo btInfo, AudioDeviceBroker audiodevicebroker, int streammusic) {
        if (audiodevicebroker != null && btInfo.mVolume != -1) {
            audiodevicebroker.postSetVolumeIndexOnDevice(streammusic, btInfo.mVolume * 10, btInfo.mAudioSystemDevice, "onSetBtActiveDevice");
        }
    }

    public String getLeAddr(LinkedHashMap<String, AudioDeviceInventory.DeviceInfo> connecteddevices) {
        if (connecteddevices == null || connecteddevices.isEmpty()) {
            return null;
        }
        for (AudioDeviceInventory.DeviceInfo di : connecteddevices.values()) {
            if (di.mDeviceType == 536870912) {
                String addr = di.mDeviceAddress;
                return addr;
            }
        }
        return null;
    }
}
