package com.android.server.audio;

import android.content.Context;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.FixedFileObserver;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class AudioThermalObserver {
    private static final String AUDIO_HIGH_TEMPERATURE_STATE_PATH = "/sys/class/thermal/thermal_message/video_mode";
    public static final int MSG_AUDIO_THERMAL_POLICY = 2500;
    public static final int MSG_CHECK_AUDIO_HIGH_TEMPERATURE_POLICY = 2502;
    public static final int MSG_SET_AUDIO_HIGH_TEMPERATURE_POLICY = 2501;
    private static final int READ_WAIT_TIME_SECONDS = 3;
    private static final String TAG = "AudioThermalObserver";
    private AudioHighTemperatureListener mAudioHighTemperatureListener;
    private Context mContext;
    private WorkHandler mHandler;
    private int mTempRecord;

    public AudioThermalObserver(Context context) {
        this.mAudioHighTemperatureListener = null;
        this.mTempRecord = 0;
        this.mTempRecord = 0;
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new WorkHandler(thread.getLooper());
        this.mAudioHighTemperatureListener = new AudioHighTemperatureListener(AUDIO_HIGH_TEMPERATURE_STATE_PATH);
        watchAudioHighTemperatureListener();
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
                        int mAudioHighTemperatureMode = msg.arg1;
                        AudioThermalObserver.this.setAudioHighTemperatureMode(mAudioHighTemperatureMode);
                        break;
                    case 2502:
                        AudioThermalObserver.this.mHandler.removeMessages(2501);
                        AudioThermalObserver.this.checkAudioHighTemperatureMode(AudioThermalObserver.AUDIO_HIGH_TEMPERATURE_STATE_PATH);
                        break;
                }
            } catch (Exception e) {
                Log.e(AudioThermalObserver.TAG, "handleMessage exception " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAudioHighTemperatureMode(int mHighTemperatureMode) {
        if (mHighTemperatureMode != 1 && mHighTemperatureMode != 0) {
            mHighTemperatureMode = 0;
        }
        if (this.mTempRecord != mHighTemperatureMode) {
            try {
                String mode = String.valueOf(mHighTemperatureMode);
                String mParameter = "Audio_High_Temperature_Mode=" + mode;
                AudioSystem.setParameters(mParameter);
                this.mTempRecord = mHighTemperatureMode;
                Log.d(TAG, "setAudioHighTemperatureMode: mHighTemperatureMode = " + mHighTemperatureMode + "; mTempRecord = " + this.mTempRecord);
                return;
            } catch (Exception e) {
                Log.e(TAG, "setAudioHighTemperatureMode exception " + e);
                return;
            }
        }
        Log.d(TAG, "AudioHighTemperatureMode is no need to change");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAudioHighTemperatureMode(String path) {
        try {
            String mMode = getContentFromFile(path);
            if (mMode == null) {
                Log.d(TAG, "HighTemperature path:" + path + "is null");
            } else {
                int mTempMode = Integer.parseInt(mMode);
                this.mHandler.removeMessages(2501);
                WorkHandler workHandler = this.mHandler;
                workHandler.sendMessage(workHandler.obtainMessage(2501, mTempMode, 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAudioHighTemperatureMode " + e);
        }
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

    private void toSleep() {
        try {
            TimeUnit.SECONDS.sleep(3L);
        } catch (Exception e) {
            Log.e(TAG, "toSleep exception: " + e);
        }
    }

    private void unwatchAudioHighTemperatureListener() {
        try {
            this.mAudioHighTemperatureListener.stopWatching();
        } catch (Exception e) {
            Log.e(TAG, "unwatchAudioHighTemperatureListener exception " + e);
        }
    }

    private void watchAudioHighTemperatureListener() {
        try {
            this.mAudioHighTemperatureListener.startWatching();
        } catch (Exception e) {
            Log.e(TAG, "watchAudioHighTemperatureListener exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AudioHighTemperatureListener extends FixedFileObserver {
        public AudioHighTemperatureListener(String path) {
            super(path);
            AudioThermalObserver.this.mHandler.sendMessage(AudioThermalObserver.this.mHandler.obtainMessage(2502));
        }

        @Override // com.android.server.FixedFileObserver
        public void onEvent(int event, String path) {
            switch (event) {
                case 2:
                    Log.d(AudioThermalObserver.TAG, "HighTemperature modify event");
                    AudioThermalObserver.this.mHandler.sendMessage(AudioThermalObserver.this.mHandler.obtainMessage(2502));
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

    public void reSetAudioCinemaModeThermal() {
        try {
            WorkHandler workHandler = this.mHandler;
            workHandler.sendMessage(workHandler.obtainMessage(2502));
        } catch (Exception e) {
            Log.e(TAG, "reSetAudioCinemaModeThermal exception " + e);
        }
    }
}
