package com.android.server.audio.pad;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

/* loaded from: classes.dex */
public class PadAdapter {
    private static final int CONFIG_PROP;
    public static final boolean ENABLE;
    private static final int FLAG_KEYBOARD_MIC_KEY_ENABLE = 4;
    private static final int FLAG_PAD_ADAPTER_ENABLE = 1;
    private static final int FLAG_VOIP_FOCUS_ENABLE = 2;
    private static final String TAG = "AudioService.PadAdapter";
    private final boolean MIC_KEY_ENABLE;
    private final boolean VOIP_FOCUS_ENABLE;
    private KeyboardMicKeyHelper mMicKeyHelper;
    private VoipFocusHelper mVoipFocusHelper;

    static {
        int i = SystemProperties.getInt("ro.vendor.audio.pad.adapter", 0);
        CONFIG_PROP = i;
        ENABLE = (i & 1) != 0;
    }

    public PadAdapter(Context context) {
        int i = CONFIG_PROP;
        boolean z = (i & 2) != 0;
        this.VOIP_FOCUS_ENABLE = z;
        boolean z2 = (i & 4) != 0;
        this.MIC_KEY_ENABLE = z2;
        Log.d(TAG, "PadAdapter Construct ...");
        if (z) {
            this.mVoipFocusHelper = new VoipFocusHelper(context);
        }
        if (z2) {
            this.mMicKeyHelper = new KeyboardMicKeyHelper(context);
        }
    }

    public void handleAudioModeUpdate(int mode) {
        if (this.VOIP_FOCUS_ENABLE) {
            this.mVoipFocusHelper.handleAudioModeUpdate(mode);
        }
    }

    public void handleMicrophoneMuteChanged(boolean muted) {
        if (this.MIC_KEY_ENABLE) {
            this.mMicKeyHelper.handleMicrophoneMuteChanged(muted);
        }
    }

    public void handleRecordEventUpdate(int audioSource) {
        if (this.MIC_KEY_ENABLE) {
            this.mMicKeyHelper.handleRecordEventUpdate(audioSource);
        }
    }

    public void handleMeetingModeUpdate(String parameter) {
        if (this.VOIP_FOCUS_ENABLE) {
            this.mVoipFocusHelper.handleMeetingModeUpdate(parameter);
        }
    }
}
