package com.android.server.audio.feature;

import android.content.Context;
import android.media.AudioPlaybackConfiguration;
import android.os.SystemProperties;
import android.util.Log;

/* loaded from: classes.dex */
public class FeatureAdapter {
    private static final int CONFIG_PROP;
    public static final boolean ENABLE;
    private static final int FLAG_FEATURE_ADAPTER_ENABLE = 1;
    private static final int FLAG_LOW_BATTERY_ENABLE = 2;
    private static final int FLAG_POWER_SAVE_ENABLE = 4;
    private static final String TAG = "AudioService.FeatureAdapter";
    private final boolean LOW_BATTERY_ENABLE;
    private final boolean POWER_SAVE_ENABLE;
    private AudioPowerSaveHelper mAudioPowerSaveHelper;

    static {
        int i = SystemProperties.getInt("ro.vendor.audio.feature.adapter", 0);
        CONFIG_PROP = i;
        ENABLE = (i & 1) != 0;
    }

    public FeatureAdapter(Context context) {
        int i = CONFIG_PROP;
        boolean z = (i & 2) != 0;
        this.LOW_BATTERY_ENABLE = z;
        boolean z2 = (i & 2) != 0;
        this.POWER_SAVE_ENABLE = z2;
        Log.d(TAG, "FeatureAdapter Construct ...");
        if (z || z2) {
            this.mAudioPowerSaveHelper = new AudioPowerSaveHelper(context);
        }
    }

    public void handleLowBattery(int batteryPct, int audioControlStatus) {
        if (this.LOW_BATTERY_ENABLE) {
            this.mAudioPowerSaveHelper.handleLowBattery(batteryPct, audioControlStatus);
        }
    }

    public void onPlayerTracked(AudioPlaybackConfiguration apc) {
        if (this.LOW_BATTERY_ENABLE) {
            this.mAudioPowerSaveHelper.initPlayerForBattery(apc);
        }
    }
}
