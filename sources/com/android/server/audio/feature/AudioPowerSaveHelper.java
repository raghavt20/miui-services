package com.android.server.audio.feature;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.util.Log;

/* loaded from: classes.dex */
public class AudioPowerSaveHelper {
    private static final int AUDIO_CONTROL_STATUS_VOLUME_CONTROL = 1;
    private static final int AUDIO_CONTROL_STATUS_VOLUME_RESUME = 0;
    private static final int BATTERY_PERCENTAGE_LOW = 10;
    private static final int BATTERY_PERCENTAGE_LOWER = 5;
    private static final String TAG = "FeatureAdapter.AudioPowerSaveHelper";
    private static final float VOLUME_HIGH_SCALE_FOR_BATTERY_LOW = 0.8f;
    private static final float VOLUME_HIGH_SCALE_FOR_BATTERY_LOWER = 0.7f;
    private static final float VOLUME_LOW_SCALE_FOR_BATTERY_LOW = 0.9f;
    private static final float VOLUME_LOW_SCALE_FOR_BATTERY_LOWER = 0.8f;
    private static final int VOLUME_STATUS_FOR_BATTERY_ADJUSTED_LOW = 1;
    private static final int VOLUME_STATUS_FOR_BATTERY_ADJUSTED_LOWER = 2;
    private static final int VOLUME_STATUS_FOR_BATTERY_NORMAL = 0;
    private AudioManager mAudioManager;
    private Context mContext;
    private int mCurVolumeStatusForBattery = 0;

    public AudioPowerSaveHelper(Context context) {
        Log.d(TAG, "AudioPowerSaveHelper Construct ...");
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public void handleLowBattery(int batteryPct, int audioControlStatus) {
        Log.d(TAG, "handleLowBattery batteryPct: " + batteryPct + ", audioControlStatus:" + audioControlStatus);
        switch (audioControlStatus) {
            case 0:
                Log.d(TAG, "AUDIO_CONTROL_STATUS_VOLUME_RESUME");
                resumeVolumeForBattery();
                return;
            case 1:
                Log.d(TAG, "AUDIO_CONTROL_STATUS_VOLUME_CONTROL batteryPct:" + batteryPct);
                controlVolumeForBattery(batteryPct);
                return;
            default:
                Log.d(TAG, "unknown audioControlStatus");
                return;
        }
    }

    private void controlVolumeForBattery(int batteryPct) {
        if (batteryPct < 0) {
            Log.d(TAG, "invalid batteryPct:" + batteryPct);
            return;
        }
        if (batteryPct > 10) {
            resumeVolumeForBattery();
            return;
        }
        if (batteryPct <= 5) {
            if (this.mCurVolumeStatusForBattery == 2) {
                return;
            } else {
                this.mCurVolumeStatusForBattery = 2;
            }
        } else if (this.mCurVolumeStatusForBattery == 1) {
            return;
        } else {
            this.mCurVolumeStatusForBattery = 1;
        }
        for (AudioPlaybackConfiguration apc : this.mAudioManager.getActivePlaybackConfigurations()) {
            adjustPlayerVolumeForBattery(apc);
        }
    }

    private void resumeVolumeForBattery() {
        if (this.mCurVolumeStatusForBattery == 0) {
            return;
        }
        Log.d(TAG, "resume player volume for battery");
        this.mCurVolumeStatusForBattery = 0;
        for (AudioPlaybackConfiguration apc : this.mAudioManager.getActivePlaybackConfigurations()) {
            adjustPlayerVolumeForBattery(apc);
        }
    }

    private void adjustPlayerVolumeForBattery(AudioPlaybackConfiguration apc) {
        int streamType = apc.getAudioAttributes().getVolumeControlStream();
        if (apc.getAudioAttributes().getUsage() == 1 || streamType == 3) {
            int maxVolume = this.mAudioManager.getStreamMaxVolume(streamType);
            int curStreamVolume = this.mAudioManager.getStreamVolume(streamType);
            float finalPlayerVolume = calculatePlayerVolumeForBattery(curStreamVolume, maxVolume);
            try {
                Log.d(TAG, "adjustPlayerVolumeForBattery. finalPlayerVolume:" + finalPlayerVolume);
                apc.getPlayerProxy().setVolume(finalPlayerVolume);
            } catch (Exception e) {
                Log.e(TAG, "adjustPlayerVolumeForBattery ", e);
            }
        }
    }

    private float calculatePlayerVolumeForBattery(int curStreamVolume, int maxVolume) {
        Log.d(TAG, "calculatePlayerVolume curStreamVolume:" + curStreamVolume + ", maxVolume:" + maxVolume + ", mCurVolumeStatusForBattery:" + this.mCurVolumeStatusForBattery);
        boolean isVolumeLow = ((double) curStreamVolume) <= ((double) maxVolume) * 0.2d;
        switch (this.mCurVolumeStatusForBattery) {
            case 1:
                if (isVolumeLow) {
                    return VOLUME_LOW_SCALE_FOR_BATTERY_LOW;
                }
                return 0.8f;
            case 2:
                if (isVolumeLow) {
                    return 0.8f;
                }
                return VOLUME_HIGH_SCALE_FOR_BATTERY_LOWER;
            default:
                return 1.0f;
        }
    }

    public void initPlayerForBattery(AudioPlaybackConfiguration apc) {
        Log.d(TAG, "initPlayerForBattery");
        if (this.mCurVolumeStatusForBattery != 0) {
            adjustPlayerVolumeForBattery(apc);
        }
    }
}
