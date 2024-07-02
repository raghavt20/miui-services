package com.android.server.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Spatializer;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.audio.dolbyeffect.DolbyEffectController;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public class MiAudioService extends AudioService implements Spatializer.OnSpatializerStateChangedListener {
    private static final String TAG = "MiAudioService";
    private final int SPATIALIZER_PARAM_SPATIALIZER_TYPE;
    private final int SPATIALIZER_PARAM_SPEAKER_ROTATION;
    private AudioManager mAudioManager;
    private DeviceChangeBroadcastReceiver mDeviceConnectStateListener;
    DolbyEffectController mDolbyEffectController;
    private ContentObserver mEffecImplementerObserver;
    private Uri mEffecImplementerUri;
    private final boolean mIsSupportedDolbyEffectControl;
    private final boolean mIsSupportedSelectedDolbyTunningByVolume;
    private Spatializer mSpatializer;
    private boolean mSpatializerEnabled;
    private int mSpatilizerType;
    private boolean mSpeakerOn;
    private final boolean mUseXiaoMiSpatilizer;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public enum SpatializerType {
        DOLBY,
        MISOUND,
        NONE
    }

    public MiAudioService(Context context) {
        super(context, AudioSystemAdapter.getDefaultAdapter(), SystemServerAdapter.getDefaultAdapter(context), SettingsAdapter.getDefaultAdapter(), new DefaultAudioPolicyFacade(), (Looper) null);
        this.SPATIALIZER_PARAM_SPEAKER_ROTATION = 304;
        this.SPATIALIZER_PARAM_SPATIALIZER_TYPE = 288;
        this.mSpatializerEnabled = false;
        this.mSpatilizerType = SpatializerType.DOLBY.ordinal();
        this.mSpeakerOn = false;
        this.mIsSupportedDolbyEffectControl = "true".equals(SystemProperties.get("vendor.audio.dolby.control.support"));
        this.mIsSupportedSelectedDolbyTunningByVolume = "true".equals(SystemProperties.get("vendor.audio.dolby.control.tunning.by.volume.support"));
        this.mUseXiaoMiSpatilizer = "true".equals(SystemProperties.get("vendor.audio.useXiaomiSpatializer"));
        Log.d(TAG, "MiAudioService()");
    }

    public void onSystemReady() {
        super.onSystemReady();
        functionsRoadedInit();
    }

    public void functionsRoadedInit() {
        if (this.mIsSupportedDolbyEffectControl) {
            DolbyEffectControllerInit();
            DeviceConectedStateListenerInit();
        }
        SettingsObserver();
        SpatialStateInit();
    }

    public void DolbyEffectControllerInit() {
        DolbyEffectController dolbyEffectController = DolbyEffectController.getInstance(this.mContext);
        this.mDolbyEffectController = dolbyEffectController;
        if (dolbyEffectController != null) {
            dolbyEffectController.init();
        }
    }

    public void SpatialStateInit() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
        this.mAudioManager = audioManager;
        if (audioManager != null) {
            this.mSpatializer = audioManager.getSpatializer();
        }
        Spatializer spatializer = this.mSpatializer;
        if (spatializer != null) {
            spatializer.addOnSpatializerStateChangedListener(Executors.newSingleThreadExecutor(), this);
        }
        String effecImplementer = Settings.Global.getString(this.mContext.getContentResolver(), "effect_implementer");
        if ("dolby".equals(effecImplementer)) {
            this.mSpatilizerType = SpatializerType.DOLBY.ordinal();
        } else if ("misound".equals(effecImplementer)) {
            this.mSpatilizerType = SpatializerType.MISOUND.ordinal();
        } else if ("none".equals(effecImplementer)) {
            this.mSpatilizerType = SpatializerType.NONE.ordinal();
        }
        Log.d(TAG, "Init EffectImplementer is " + effecImplementer);
    }

    public void DeviceConectedStateListenerInit() {
        this.mDeviceConnectStateListener = new DeviceChangeBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        filter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        filter.addCategory("android.bluetooth.headset.intent.category.companyid." + Integer.toString(911));
        this.mContext.registerReceiver(this.mDeviceConnectStateListener, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeviceChangeBroadcastReceiver extends BroadcastReceiver {
        private DeviceChangeBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (MiAudioService.this.mDolbyEffectController != null) {
                Log.d(MiAudioService.TAG, "DeviceChangeBroadcastReceiver onReceive");
                MiAudioService.this.mDolbyEffectController.receiveDeviceConnectStateChanged(context, intent);
            }
        }
    }

    public void SettingsObserver() {
        this.mEffecImplementerUri = Settings.Global.getUriFor("effect_implementer");
        this.mEffecImplementerObserver = new ContentObserver(null) { // from class: com.android.server.audio.MiAudioService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange);
                String effecImplementer = Settings.Global.getString(MiAudioService.this.mContext.getContentResolver(), "effect_implementer");
                if ("dolby".equals(effecImplementer)) {
                    MiAudioService.this.mSpatilizerType = SpatializerType.DOLBY.ordinal();
                } else if ("misound".equals(effecImplementer)) {
                    MiAudioService.this.mSpatilizerType = SpatializerType.MISOUND.ordinal();
                } else if ("none".equals(effecImplementer)) {
                    MiAudioService.this.mSpatilizerType = SpatializerType.NONE.ordinal();
                }
                Log.d(MiAudioService.TAG, "Current EffectImplementer is " + effecImplementer);
                if (MiAudioService.this.mUseXiaoMiSpatilizer && MiAudioService.this.isSpatializerEnabled() && MiAudioService.this.isSpatializerAvailable()) {
                    Log.d(MiAudioService.TAG, "SettingsObserver setSpatializerParameter spatilizerType = " + MiAudioService.this.mSpatilizerType);
                    try {
                        MiAudioService miAudioService = MiAudioService.this;
                        miAudioService.setSpatializerParameter(288, miAudioService.intToBytes(miAudioService.mSpatilizerType));
                    } catch (Exception e) {
                        Log.e(MiAudioService.TAG, "SettingsObserver Exception " + e);
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(this.mEffecImplementerUri, true, this.mEffecImplementerObserver);
    }

    @Override // android.media.Spatializer.OnSpatializerStateChangedListener
    public void onSpatializerEnabledChanged(Spatializer spat, boolean enabled) {
        this.mSpatializerEnabled = enabled;
        if (this.mDolbyEffectController != null) {
            Log.d(TAG, "onSpatializerEnabledChanged: enabled = " + enabled);
            this.mDolbyEffectController.receiveSpatializerEnabledChanged(enabled);
        }
        if (this.mUseXiaoMiSpatilizer && this.mSpatializerEnabled) {
            try {
                Log.d(TAG, "onSpatializerEnabledChanged: setSpatializerParameter mSpatilizerType = " + this.mSpatilizerType);
                setSpatializerParameter(288, intToBytes(this.mSpatilizerType));
            } catch (Exception e) {
                Log.e(TAG, "onSpatializerEnabledChanged Exception:" + e);
            }
        }
        Log.d(TAG, "onSpatializerEnabledChanged notifyEffectChanged");
        Intent intent = new Intent("miui.intent.action.ACTION_AUDIO_EFFECT_CHANGED");
        this.mContext.sendBroadcast(intent);
    }

    @Override // android.media.Spatializer.OnSpatializerStateChangedListener
    public void onSpatializerAvailableChanged(Spatializer spat, boolean available) {
        if (this.mDolbyEffectController != null) {
            Log.d(TAG, "onSpatializerAvailableChanged: available = " + available);
            this.mDolbyEffectController.receiveSpatializerAvailableChanged(available);
        }
    }

    public void onRotationUpdate(Integer rotation) {
        super.onRotationUpdate(rotation);
        if (this.mDolbyEffectController != null) {
            Log.d(TAG, "onRotationUpdate, receiveRotationChanged");
            this.mDolbyEffectController.receiveRotationChanged(rotation.intValue());
        }
        if (this.mUseXiaoMiSpatilizer && isSpatializerEnabled() && isSpatializerAvailable()) {
            byte[] Rotation = intToBytes(rotation.intValue());
            try {
                Log.d(TAG, "setSpatializerParameter SPATIALIZER_PARAM_SPEAKER_ROTATION = " + rotation);
                setSpatializerParameter(304, Rotation);
            } catch (Exception e) {
                Log.e(TAG, "setSpatializerParameter SPATIALIZER_PARAM_SPEAKER_ROTATION Exception " + e);
            }
        }
    }

    protected void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage, String caller, int uid, int pid, String attributionTag, boolean hasModifyAudioSettings, int keyEventMode) {
        super.adjustStreamVolume(streamType, direction, flags, callingPackage, caller, uid, pid, attributionTag, hasModifyAudioSettings, keyEventMode);
        if (this.mDolbyEffectController != null && this.mIsSupportedSelectedDolbyTunningByVolume && streamType == 3) {
            int newVolume = getDeviceStreamVolume(3, 2);
            Log.d(TAG, "adjustStreamVolume");
            this.mDolbyEffectController.receiveVolumeChanged(newVolume);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] intToBytes(int src) {
        byte[] result = {(byte) (src & 255), (byte) ((src >> 8) & 255), (byte) ((src >> 16) & 255), (byte) ((src >> 24) & 255)};
        return result;
    }
}
