package com.android.server.audio;

import android.media.AudioDeviceAttributes;
import android.media.AudioSystem;
import android.media.audiofx.MiSound;
import android.util.Log;
import com.android.server.audio.AudioDeviceBroker;
import com.miui.base.MiuiStubRegistry;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class AudioDeviceBrokerStubImpl implements AudioDeviceBrokerStub {
    private static final String TAG = "AudioDeviceBrokerStubImpl";
    private boolean isappbleneed = false;
    private MiSound mMiSound;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AudioDeviceBrokerStubImpl> {

        /* compiled from: AudioDeviceBrokerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AudioDeviceBrokerStubImpl INSTANCE = new AudioDeviceBrokerStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AudioDeviceBrokerStubImpl m725provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AudioDeviceBrokerStubImpl m724provideNewInstance() {
            return new AudioDeviceBrokerStubImpl();
        }
    }

    public boolean isSetSpeakerphoneOn(boolean isLeDeviceConnected, boolean speakerOn, int pid, boolean isBuiltinSpkType) {
        if (isLeDeviceConnected && !speakerOn) {
            Log.v(TAG, "setSpeakerphoneOn, on: " + speakerOn + " pid: " + pid + "when ble is connect. getCommunicationRouteClientForPid directly to avoid waiting too long");
            if (isBuiltinSpkType) {
                Log.v(TAG, "setSpeakerphoneOn, on: " + speakerOn + " pid: " + pid + "when ble is connect return.");
                return true;
            }
            return false;
        }
        return false;
    }

    public void updateBtScoAudioHelper(boolean wasblescorequested, boolean isblescorequested, BtHelper bthelper, boolean iseventsourcestartswith) {
        Log.d(TAG, "setCommunicationRouteForClient for LEA " + wasblescorequested + " " + isblescorequested);
        if (wasblescorequested && !isblescorequested && !iseventsourcestartswith) {
            bthelper.disconnectBluetoothScoAudioHelper();
        } else if (!wasblescorequested && isblescorequested && !iseventsourcestartswith) {
            bthelper.connectBluetoothScoAudioHelper();
        }
    }

    public String getDeviceAttrAddr(LinkedList<AudioDeviceBroker.CommunicationRouteClient> mCommunicationRouteClients, int audiodeviceBLEHeadset, AudioDeviceInventory deviceinventory) {
        String address;
        if (!mCommunicationRouteClients.isEmpty()) {
            Iterator<AudioDeviceBroker.CommunicationRouteClient> iterator = mCommunicationRouteClients.iterator();
            while (iterator.hasNext()) {
                AudioDeviceBroker.CommunicationRouteClient cl = iterator.next();
                if (cl.getDevice() != null && cl.getDevice().getType() == audiodeviceBLEHeadset && (address = deviceinventory.getLeAudioAddress()) != null) {
                    Log.v(TAG, "preferredCommunicationDevice, return ble device when ble is selected by other app");
                    return address;
                }
            }
            return null;
        }
        return null;
    }

    public void setBTBLEParameters(AudioDeviceAttributes preferredCommunicationDevice, int audiodeviceBLEHeadset) {
        if ((preferredCommunicationDevice == null || preferredCommunicationDevice.getType() != audiodeviceBLEHeadset) && this.isappbleneed) {
            this.isappbleneed = false;
            AudioSystem.setParameters("BT_BLE=off");
        } else if (preferredCommunicationDevice != null && preferredCommunicationDevice.getType() == audiodeviceBLEHeadset && !this.isappbleneed) {
            this.isappbleneed = true;
            AudioSystem.setParameters("BT_BLE=on");
        }
    }

    public void setMiSoundEnable(boolean enable) {
        MiSound miSound;
        try {
            try {
                MiSound miSound2 = new MiSound(0, 0);
                this.mMiSound = miSound2;
                miSound2.setEnabled(enable);
                miSound = this.mMiSound;
                if (miSound == null) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "updateMisoundTunning: IllegalArgumentException" + e);
                miSound = this.mMiSound;
                if (miSound == null) {
                    return;
                }
            } catch (UnsupportedOperationException e2) {
                Log.e(TAG, "updateMisoundTunning: UnsupportedOperationException" + e2);
                miSound = this.mMiSound;
                if (miSound == null) {
                    return;
                }
            } catch (RuntimeException e3) {
                Log.e(TAG, "updateMisoundTunning: RuntimeException" + e3);
                miSound = this.mMiSound;
                if (miSound == null) {
                    return;
                }
            }
            miSound.release();
        } catch (Throwable th) {
            MiSound miSound3 = this.mMiSound;
            if (miSound3 != null) {
                miSound3.release();
            }
            throw th;
        }
    }
}
