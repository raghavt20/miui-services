package com.android.server.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.FixedFileObserver;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class AudioPowerSaveModeObserver {
    private static final String ACTION_POWER_SAVE_MODE_CHANGED = "miui.intent.action.POWER_SAVE_MODE_CHANGED";
    private static final String AUDIO_POWER_SAVE_LEVEL_PATH = "/sys/class/thermal/power_save/power_level";
    private static final String AUDIO_POWER_SAVE_SETTING = "persist.vendor.audio.power.save.setting";
    private static final String AUDIO_POWER_SAVE_STATE_PATH = "/sys/class/thermal/power_save/powersave_mode";
    private static final String KEY_GLOBAL_VOICE_TRIGGER_ENABLED = "global_voice_trigger_enabled";
    private static final String METHOD_START_RECOGNITION = "method_start_recognition";
    private static final String METHOD_STOP_RECOGNITION = "method_stop_recognition";
    public static final int MSG_AUDIO_POWER_SAVE_POLICY = 2500;
    public static final int MSG_CHECK_AUDIO_POWER_SAVE_POLICY = 2504;
    public static final int MSG_CLEAR_AUDIO_POWER_SAVE_POLICY = 2503;
    public static final int MSG_SET_AUDIO_POWER_SAVE_LEVEL_POLICY = 2502;
    public static final int MSG_SET_AUDIO_POWER_SAVE_MODE_POLICY = 2501;
    private static final int READ_WAIT_TIME_SECONDS = 3;
    private static final String TAG = "AudioPowerSaveModeObserver";
    private static final int VALUE_GLOBAL_VOICE_TRIGGER_DISABLE = 0;
    private static final int VALUE_GLOBAL_VOICE_TRIGGER_ENABLED = 1;
    private static int mIsVoiceTriggerEnableForAudioPowerSave = -1;
    private static int mIsVoiceTriggerEnableForGlobal = -1;
    private AudioPowerSaveStateListener mAudioPowerLevelListener;
    private AudioPowerSaveStateListener mAudioPowerSaveListener;
    private Context mContext;
    private WorkHandler mHandler;
    private int mLevelRecord;
    private int mModeRecord;
    private int mVoiceTriggerRecord = 0;
    private Uri mUri = Uri.parse("content://com.miui.voicetrigger.SmModelProvider/mode/voicetrigger");
    private BroadcastReceiver mAudioPowerSaveBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.audio.AudioPowerSaveModeObserver.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                if (action.equals(AudioPowerSaveModeObserver.ACTION_POWER_SAVE_MODE_CHANGED)) {
                    Log.d(AudioPowerSaveModeObserver.TAG, "mAudioPowerSaveBroadcastReceiver: ACTION_POWER_SAVE_MODE_CHANGED");
                    AudioPowerSaveModeObserver.this.mHandler.sendMessage(AudioPowerSaveModeObserver.this.mHandler.obtainMessage(AudioPowerSaveModeObserver.MSG_CHECK_AUDIO_POWER_SAVE_POLICY));
                } else {
                    Log.e(AudioPowerSaveModeObserver.TAG, "mAudioPowerSaveBroadcastReceiver error ");
                }
            } catch (Exception e) {
                Log.e(AudioPowerSaveModeObserver.TAG, "mAudioPowerSaveBroadcastReceiver exception " + e);
            }
        }
    };

    public AudioPowerSaveModeObserver(Context context) {
        this.mAudioPowerSaveListener = null;
        this.mAudioPowerLevelListener = null;
        this.mModeRecord = 0;
        this.mLevelRecord = 0;
        this.mModeRecord = 0;
        this.mLevelRecord = 0;
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new WorkHandler(thread.getLooper());
        this.mAudioPowerSaveListener = new AudioPowerSaveStateListener(AUDIO_POWER_SAVE_STATE_PATH);
        this.mAudioPowerLevelListener = new AudioPowerSaveStateListener(AUDIO_POWER_SAVE_LEVEL_PATH);
        watchAudioPowerStateListener();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 2501:
                        int mAudioPowerSaveLevel = msg.arg1;
                        Log.d(AudioPowerSaveModeObserver.TAG, "MSG_SET_AUDIO_POWER_SAVE_MODE_POLICY, AudioPowerSaveMode is " + mAudioPowerSaveLevel);
                        AudioPowerSaveModeObserver.this.setAudioPowerSaveMode(mAudioPowerSaveLevel);
                        break;
                    case 2502:
                        int mAudioPowerSaveLevel2 = msg.arg1;
                        Log.d(AudioPowerSaveModeObserver.TAG, "MSG_SET_AUDIO_POWER_SAVE_LEVEL_POLICY, AudioPowerSaveLevel is " + mAudioPowerSaveLevel2);
                        AudioPowerSaveModeObserver.this.setAudioPowerSaveLevel(mAudioPowerSaveLevel2);
                        break;
                    case AudioPowerSaveModeObserver.MSG_CLEAR_AUDIO_POWER_SAVE_POLICY /* 2503 */:
                        Log.d(AudioPowerSaveModeObserver.TAG, "MSG_CLEAR_AUDIO_POWER_SAVE_POLICY");
                        AudioPowerSaveModeObserver.this.mHandler.removeMessages(2501);
                        AudioPowerSaveModeObserver.this.mHandler.removeMessages(2502);
                        AudioPowerSaveModeObserver.this.setAudioPowerSaveMode(0);
                        AudioPowerSaveModeObserver.this.setAudioPowerSaveLevel(0);
                        break;
                    case AudioPowerSaveModeObserver.MSG_CHECK_AUDIO_POWER_SAVE_POLICY /* 2504 */:
                        Log.d(AudioPowerSaveModeObserver.TAG, "MSG_CHECK_AUDIO_POWER_SAVE_POLICY");
                        AudioPowerSaveModeObserver.this.mHandler.removeMessages(2501);
                        AudioPowerSaveModeObserver.this.mHandler.removeMessages(2502);
                        if (AudioPowerSaveModeObserver.this.checkAudioPowerSaveState(AudioPowerSaveModeObserver.AUDIO_POWER_SAVE_STATE_PATH) != 0) {
                            AudioPowerSaveModeObserver.this.toSleep();
                            AudioPowerSaveModeObserver.this.checkAudioPowerSaveLevel(AudioPowerSaveModeObserver.AUDIO_POWER_SAVE_LEVEL_PATH);
                            break;
                        } else {
                            AudioPowerSaveModeObserver.this.mHandler.sendMessage(AudioPowerSaveModeObserver.this.mHandler.obtainMessage(AudioPowerSaveModeObserver.MSG_CLEAR_AUDIO_POWER_SAVE_POLICY));
                            Log.d(AudioPowerSaveModeObserver.TAG, "PowerSaveMode is close now");
                            break;
                        }
                }
            } catch (Exception e) {
                Log.e(AudioPowerSaveModeObserver.TAG, "handleMessage exception " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAudioPowerSaveMode(int mPowerSaveMode) {
        if (mPowerSaveMode == -1) {
            mPowerSaveMode = 0;
        }
        try {
            String mode = String.valueOf(mPowerSaveMode);
            if (mode.length() == 1) {
                mode = "0" + mode;
            }
            if (this.mModeRecord != mPowerSaveMode) {
                Log.d(TAG, "natural mode =  " + mode);
                String mParameter = "Audio_Power_Save_Mode=" + mode.charAt(1);
                AudioSystem.setParameters(mParameter);
                this.mModeRecord = mPowerSaveMode;
            } else {
                Log.d(TAG, "AudioPowerSaveMode is no need to change");
            }
            int currentAudioPowerSave = SystemProperties.getInt(AUDIO_POWER_SAVE_SETTING, 0);
            if ((currentAudioPowerSave & 8) != 0) {
                setAudioPowerSaveVoiceTrigger(mode);
            }
        } catch (Exception e) {
            Log.e(TAG, "setAudioPowerSaveMode exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAudioPowerSaveLevel(int mPowerSaveLevel) {
        if (mPowerSaveLevel == -1) {
            mPowerSaveLevel = 0;
        }
        if (this.mModeRecord == 0) {
            Log.d(TAG, "PowerMode is close, setPowerLevel 0");
            mPowerSaveLevel = 0;
        }
        if (this.mLevelRecord != mPowerSaveLevel) {
            try {
                String level = String.valueOf(mPowerSaveLevel);
                String mParameter = "Audio_Power_Save_Level=" + level;
                AudioSystem.setParameters(mParameter);
                this.mLevelRecord = mPowerSaveLevel;
            } catch (Exception e) {
                Log.e(TAG, "setAudioPowerSaveLevel exception " + e);
            }
        }
    }

    private void setAudioPowerSaveVoiceTrigger(String powersavemode) {
        if (!isSupportSetVoiceTrigger(powersavemode)) {
            Log.d(TAG, "no need to change VoiceTrigger");
            return;
        }
        try {
            Bundle bundle = new Bundle();
            if (powersavemode.substring(1, 2).equals("1")) {
                Log.d(TAG, "setVoiceTrigger stop");
                this.mContext.getContentResolver().call(this.mUri, METHOD_STOP_RECOGNITION, (String) null, bundle);
                mIsVoiceTriggerEnableForAudioPowerSave = 0;
            } else {
                Log.d(TAG, "setVoiceTrigger start");
                this.mContext.getContentResolver().call(this.mUri, METHOD_START_RECOGNITION, (String) null, bundle);
                mIsVoiceTriggerEnableForAudioPowerSave = 1;
            }
        } catch (Exception e) {
            Log.e(TAG, "setVoiceTrigger exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int checkAudioPowerSaveState(String path) {
        int mEnable = 0;
        try {
            String mState = getContentFromFile(path);
            if (mState == null) {
                Log.d(TAG, "path:" + path + " is null");
            } else {
                int mPowerSave = Integer.parseInt(mState);
                mEnable = mPowerSave;
                checkAudioPowerSaveVoiceTrigger();
                this.mHandler.removeMessages(2501);
                WorkHandler workHandler = this.mHandler;
                workHandler.sendMessageDelayed(workHandler.obtainMessage(2501, mPowerSave, 0), 1000L);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAudioPowerSaveState " + e);
        }
        return mEnable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAudioPowerSaveLevel(String path) {
        int mPowerLevel;
        try {
            String mLevel = getContentFromFile(path);
            if (mLevel == null) {
                Log.d(TAG, "path:" + path + " is null");
                return;
            }
            if (!"0".equals(mLevel) && !"-1".equals(mLevel)) {
                mPowerLevel = Integer.parseInt(mLevel.substring(6, 7));
                Log.d(TAG, "checkAudioPowerSaveLevel mPowerLevel is: " + mPowerLevel + " in " + mLevel.substring(0, 6) + "{" + mLevel.substring(6, 7) + "}" + mLevel.substring(7, 9));
                this.mHandler.removeMessages(2502);
                WorkHandler workHandler = this.mHandler;
                workHandler.sendMessageDelayed(workHandler.obtainMessage(2502, mPowerLevel, 0), 1000L);
            }
            mPowerLevel = Integer.parseInt(mLevel);
            this.mHandler.removeMessages(2502);
            WorkHandler workHandler2 = this.mHandler;
            workHandler2.sendMessageDelayed(workHandler2.obtainMessage(2502, mPowerLevel, 0), 1000L);
        } catch (Exception e) {
            Log.e(TAG, "checkAudioPowerSaveLevel " + e);
        }
    }

    private void checkAudioPowerSaveVoiceTrigger() {
        Log.d(TAG, "enter checkAudioPowerSaveVoiceTrigger");
        int isVoicetriggerenablel = Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_GLOBAL_VOICE_TRIGGER_ENABLED, 0);
        mIsVoiceTriggerEnableForGlobal = isVoicetriggerenablel;
    }

    private boolean isSupportSetVoiceTrigger(String powersavemode) {
        String voicetriggermode = powersavemode.substring(0, 1);
        String audiopowersavemode = powersavemode.substring(1, 2);
        if (audiopowersavemode.equals("1")) {
            this.mVoiceTriggerRecord = Integer.parseInt(voicetriggermode);
        }
        if (this.mVoiceTriggerRecord == 0) {
            Log.d(TAG, "VoiceTriggerMode is disable now, do not setVoiceTrigger");
            mIsVoiceTriggerEnableForAudioPowerSave = -1;
            return false;
        }
        if (audiopowersavemode.equals("1") && mIsVoiceTriggerEnableForGlobal == 0) {
            Log.d(TAG, "open audiopowersavemode, VoiceTrigger is disable now, do not setVoiceTrigger");
            mIsVoiceTriggerEnableForAudioPowerSave = -1;
            return false;
        }
        if (!audiopowersavemode.equals("0") || mIsVoiceTriggerEnableForGlobal == mIsVoiceTriggerEnableForAudioPowerSave) {
            return true;
        }
        Log.d(TAG, "close powersavemode, VoiceTrigger is diff now, do not setVoiceTrigger");
        mIsVoiceTriggerEnableForAudioPowerSave = -1;
        return false;
    }

    private String getContentFromFile(String filePath) {
        StringBuilder sb;
        FileInputStream is = null;
        String content = "";
        try {
            try {
                File file = new File(filePath);
                is = new FileInputStream(file);
                byte[] data = readInputStream(is);
                content = new String(data).trim();
                try {
                    is.close();
                } catch (IOException e) {
                    e = e;
                    sb = new StringBuilder();
                    Log.w(TAG, sb.append("can not get temp state ").append(e).toString());
                    return content;
                }
            } catch (Throwable th) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                        Log.w(TAG, "can not get temp state " + e2);
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e3) {
            Log.w(TAG, "can't find file " + filePath + e3);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                    Log.w(TAG, sb.append("can not get temp state ").append(e).toString());
                    return content;
                }
            }
        } catch (IOException e5) {
            Log.w(TAG, "IO exception when read file " + filePath);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    e = e6;
                    sb = new StringBuilder();
                    Log.w(TAG, sb.append("can not get temp state ").append(e).toString());
                    return content;
                }
            }
        } catch (IndexOutOfBoundsException e7) {
            Log.w(TAG, "index exception: " + e7);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e8) {
                    e = e8;
                    sb = new StringBuilder();
                    Log.w(TAG, sb.append("can not get temp state ").append(e).toString());
                    return content;
                }
            }
        }
        return content;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toSleep() {
        try {
            TimeUnit.SECONDS.sleep(3L);
        } catch (Exception e) {
            Log.e(TAG, "toSleep exception: " + e);
        }
    }

    private void watchAudioPowerStateListener() {
        try {
            this.mAudioPowerSaveListener.startWatching();
            this.mAudioPowerLevelListener.startWatching();
        } catch (Exception e) {
            Log.e(TAG, "watchAudioPowerStateListener exception " + e);
        }
    }

    private void unwatchAudioPowerStateListener() {
        try {
            this.mAudioPowerSaveListener.stopWatching();
            this.mAudioPowerLevelListener.stopWatching();
        } catch (Exception e) {
            Log.e(TAG, "unwatchAudioPowerStateListener exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AudioPowerSaveStateListener extends FixedFileObserver {
        public AudioPowerSaveStateListener(String path) {
            super(path);
            AudioPowerSaveModeObserver.this.mHandler.sendMessage(AudioPowerSaveModeObserver.this.mHandler.obtainMessage(AudioPowerSaveModeObserver.MSG_CHECK_AUDIO_POWER_SAVE_POLICY));
        }

        @Override // com.android.server.FixedFileObserver
        public void onEvent(int event, String path) {
            switch (event) {
                case 2:
                    Log.d(AudioPowerSaveModeObserver.TAG, "MODIFY event");
                    AudioPowerSaveModeObserver.this.mHandler.sendMessage(AudioPowerSaveModeObserver.this.mHandler.obtainMessage(AudioPowerSaveModeObserver.MSG_CHECK_AUDIO_POWER_SAVE_POLICY));
                    return;
                default:
                    return;
            }
        }
    }

    private static byte[] readInputStream(FileInputStream is) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        while (true) {
            try {
                try {
                    int count = is.read(buffer, 0, 512);
                    if (count <= 0) {
                        break;
                    }
                    byteStream.write(buffer, 0, count);
                } catch (Exception e) {
                    Log.w(TAG, "readInputStream " + e);
                    try {
                        byteStream.close();
                        return null;
                    } catch (IOException e2) {
                        Log.w(TAG, "readInputStream " + e2);
                        return null;
                    }
                }
            } catch (Throwable th) {
                try {
                    byteStream.close();
                } catch (IOException e3) {
                    Log.w(TAG, "readInputStream " + e3);
                }
                throw th;
            }
        }
        byte[] byteArray = byteStream.toByteArray();
        try {
            byteStream.close();
        } catch (IOException e4) {
            Log.w(TAG, "readInputStream " + e4);
        }
        return byteArray;
    }

    private void registerBroadCastReceiver() {
        try {
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(ACTION_POWER_SAVE_MODE_CHANGED);
            this.mContext.registerReceiver(this.mAudioPowerSaveBroadcastReceiver, mIntentFilter);
            Log.d(TAG, "registerBroadCastReceiver");
        } catch (Exception e) {
            Log.e(TAG, "registerBroadCastReceiver exception " + e);
        }
    }

    private void unregisterBroadCastReceiver() {
        try {
            this.mContext.unregisterReceiver(this.mAudioPowerSaveBroadcastReceiver);
            Log.d(TAG, "unregisterBroadCastReceiver");
        } catch (Exception e) {
            Log.e(TAG, "unregisterBroadCastReceiver exception " + e);
        }
    }

    public void reSetAudioPowerParam() {
        try {
            WorkHandler workHandler = this.mHandler;
            workHandler.sendMessage(workHandler.obtainMessage(MSG_CHECK_AUDIO_POWER_SAVE_POLICY));
        } catch (Exception e) {
            Log.e(TAG, "reSetAudioPowerParam exception " + e);
        }
    }
}
