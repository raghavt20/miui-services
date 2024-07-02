package com.android.server.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Spatializer;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.audio.dolbyeffect.DolbyEffectController;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public class MiAudioServiceMTK implements Spatializer.OnSpatializerStateChangedListener {
    private static final String TAG = "MiAudioServiceMTK";
    private AudioManager mAudioManager;
    private AudioService mAudioService;
    final Context mContext;
    private DeviceChangeBroadcastReceiver mDeviceConnectStateListener;
    DolbyEffectController mDolbyEffectController;
    private ContentObserver mEffecImplementerObserver;
    private Uri mEffecImplementerUri;
    private Spatializer mSpatializer;
    private final int SPATIALIZER_PARAM_SPEAKER_ROTATION = 304;
    private final int SPATIALIZER_PARAM_SPATIALIZER_TYPE = 288;
    private boolean mSpatializerEnabled = false;
    private int mSpatilizerType = SpatializerType.DOLBY.ordinal();
    private final boolean mIsSupportedDolbyEffectControl = "true".equals(SystemProperties.get("vendor.audio.dolby.control.support"));
    private final boolean mIsSupportedSelectedDolbyTunningByVolume = "true".equals(SystemProperties.get("vendor.audio.dolby.control.tunning.by.volume.support"));
    private final boolean mUseXiaoMiSpatilizer = "true".equals(SystemProperties.get("vendor.audio.useXiaomiSpatializer"));

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public enum SpatializerType {
        DOLBY,
        MISOUND,
        NONE
    }

    public MiAudioServiceMTK(Context context, AudioService audioservice) {
        this.mContext = context;
        this.mAudioService = audioservice;
        Log.d(TAG, "MiAudioServiceMTK()");
    }

    public void onSystemReady() {
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
            if (MiAudioServiceMTK.this.mDolbyEffectController != null) {
                Log.d(MiAudioServiceMTK.TAG, "DeviceChangeBroadcastReceiver onReceive");
                MiAudioServiceMTK.this.mDolbyEffectController.receiveDeviceConnectStateChanged(context, intent);
            }
        }
    }

    public void SettingsObserver() {
        this.mEffecImplementerUri = Settings.Global.getUriFor("effect_implementer");
        this.mEffecImplementerObserver = new ContentObserver(null) { // from class: com.android.server.audio.MiAudioServiceMTK.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange);
                String effecImplementer = Settings.Global.getString(MiAudioServiceMTK.this.mContext.getContentResolver(), "effect_implementer");
                if ("dolby".equals(effecImplementer)) {
                    MiAudioServiceMTK.this.mSpatilizerType = SpatializerType.DOLBY.ordinal();
                } else if ("misound".equals(effecImplementer)) {
                    MiAudioServiceMTK.this.mSpatilizerType = SpatializerType.MISOUND.ordinal();
                } else if ("none".equals(effecImplementer)) {
                    MiAudioServiceMTK.this.mSpatilizerType = SpatializerType.NONE.ordinal();
                }
                Log.d(MiAudioServiceMTK.TAG, "Current EffectImplementer is " + effecImplementer);
                if (MiAudioServiceMTK.this.mUseXiaoMiSpatilizer && MiAudioServiceMTK.this.mAudioService != null && MiAudioServiceMTK.this.mAudioService.isSpatializerEnabled() && MiAudioServiceMTK.this.mAudioService.isSpatializerAvailable()) {
                    Log.d(MiAudioServiceMTK.TAG, "SettingsObserver setSpatializerParameter spatilizerType = " + MiAudioServiceMTK.this.mSpatilizerType);
                    try {
                        AudioService audioService = MiAudioServiceMTK.this.mAudioService;
                        MiAudioServiceMTK miAudioServiceMTK = MiAudioServiceMTK.this;
                        audioService.setSpatializerParameter(288, miAudioServiceMTK.intToBytes(miAudioServiceMTK.mSpatilizerType));
                    } catch (Exception e) {
                        Log.e(MiAudioServiceMTK.TAG, "SettingsObserver Exception " + e);
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
                AudioService audioService = this.mAudioService;
                if (audioService != null) {
                    audioService.setSpatializerParameter(288, intToBytes(this.mSpatilizerType));
                }
            } catch (Exception e) {
                Log.e(TAG, "onSpatializerEnabledChanged Exception:" + e);
            }
        }
    }

    @Override // android.media.Spatializer.OnSpatializerStateChangedListener
    public void onSpatializerAvailableChanged(Spatializer spat, boolean available) {
        if (this.mDolbyEffectController != null) {
            Log.d(TAG, "onSpatializerAvailableChanged: available = " + available);
            this.mDolbyEffectController.receiveSpatializerAvailableChanged(available);
        }
    }

    public void onRotationUpdate(Integer rotation) {
        AudioService audioService;
        if (this.mDolbyEffectController != null) {
            Log.d(TAG, "onRotationUpdate, receiveRotationChanged");
            this.mDolbyEffectController.receiveRotationChanged(rotation.intValue());
        }
        if (this.mUseXiaoMiSpatilizer && (audioService = this.mAudioService) != null && audioService.isSpatializerEnabled() && this.mAudioService.isSpatializerAvailable()) {
            byte[] Rotation = intToBytes(rotation.intValue());
            try {
                Log.d(TAG, "setSpatializerParameter SPATIALIZER_PARAM_SPEAKER_ROTATION = " + rotation);
                this.mAudioService.setSpatializerParameter(304, Rotation);
            } catch (Exception e) {
                Log.e(TAG, "setSpatializerParameter SPATIALIZER_PARAM_SPEAKER_ROTATION Exception " + e);
            }
        }
    }

    public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage, String caller, int uid, int pid, String attributionTag, boolean hasModifyAudioSettings, int keyEventMode) {
        AudioService audioService;
        if (this.mDolbyEffectController != null && this.mIsSupportedSelectedDolbyTunningByVolume && streamType == 3 && (audioService = this.mAudioService) != null) {
            int newVolume = audioService.getDeviceStreamVolume(3, 2);
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
