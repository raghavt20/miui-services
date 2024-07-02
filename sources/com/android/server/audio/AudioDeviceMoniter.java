package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;

/* loaded from: classes.dex */
public class AudioDeviceMoniter {
    private static final String ACTION_DISABLE_NFC_POLLING = "com.android.nfc.action.DISABLE_POLLING";
    private static final String ACTION_ENABLE_NFC_POLLING = "com.android.nfc.action.ENABLE_POLLING";
    public static final int AUDIO_STATE_OFF = 0;
    public static final int AUDIO_STATE_ON = 1;
    private static final String PROPERTY_MIC_STATUS = "vendor.audio.mic.status";
    private static final String PROPERTY_RCV_STATUS = "vendor.audio.receiver.status";
    private static final String TAG = "AudioDeviceMoniter";
    private static volatile AudioDeviceMoniter sInstance;
    private Intent mAction;
    private Context mContext;
    private int mCurrentNfcPollState;

    private AudioDeviceMoniter(Context context) {
        Log.d(TAG, "AudioDeviceMoniter init...");
        this.mContext = context;
        Log.d(TAG, "AudioDeviceMoniter init done");
    }

    public static AudioDeviceMoniter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AudioDeviceMoniter(context);
        }
        return sInstance;
    }

    public final void startPollAudioMicStatus() {
        Thread MicAndRcvPollThread = new Thread() { // from class: com.android.server.audio.AudioDeviceMoniter.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                AudioDeviceMoniter.this.pollMicAndRcv();
            }
        };
        MicAndRcvPollThread.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pollMicAndRcv() {
        while (true) {
            String AudioMicState = SystemProperties.get(PROPERTY_MIC_STATUS);
            String AudioRcvState = SystemProperties.get(PROPERTY_RCV_STATUS);
            int micState = 0;
            int rcvState = 0;
            boolean isMicStatusInvalid = false;
            boolean isRcvStatusInvalid = false;
            if (AudioMicState.equals("on")) {
                micState = 1;
            } else if (AudioMicState.equals("off")) {
                micState = 0;
            } else {
                isMicStatusInvalid = true;
            }
            if (AudioRcvState.equals("on")) {
                rcvState = 1;
            } else if (AudioRcvState.equals("off")) {
                rcvState = 0;
            } else {
                isRcvStatusInvalid = true;
            }
            if (isMicStatusInvalid && isRcvStatusInvalid) {
                Log.w(TAG, "unexpected value for AudioRcvState and AudioMicState.");
                return;
            }
            int mPollState = rcvState | micState;
            if (mPollState != this.mCurrentNfcPollState) {
                if (mPollState != 0) {
                    this.mAction = new Intent(ACTION_DISABLE_NFC_POLLING);
                } else {
                    this.mAction = new Intent(ACTION_ENABLE_NFC_POLLING);
                }
                Log.d(TAG, "nfc status be changed to " + mPollState + ", sent broadcast to nfc...");
                this.mContext.sendBroadcastAsUser(this.mAction, UserHandle.ALL);
                this.mCurrentNfcPollState = mPollState;
            }
            SystemClock.sleep(500L);
        }
    }
}
