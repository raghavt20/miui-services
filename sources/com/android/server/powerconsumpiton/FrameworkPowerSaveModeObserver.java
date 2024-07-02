package com.android.server.powerconsumpiton;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/* loaded from: classes.dex */
class FrameworkPowerSaveModeObserver {
    private static final int MSG_FRAMEWORK_POWER_SAVE_POLICY = 1000;
    private static final int MSG_SET_FRAMEWORK_POWER_SAVE_POLICY = 1001;
    private static final String POWERSAVE_MODE_PATH = "/sys/class/thermal/power_save/powersave_mode";
    private static final String TAG = "FrameworkPowerSaveModeObserver";
    private final Context mContext;
    private Handler mHandler;
    private PowerSaveModeListener mPowerSaveModeListener = null;
    private boolean mIsPowerSaveMode = false;

    public FrameworkPowerSaveModeObserver(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new WorkHandler(looper);
        initPowerSaveModeListener();
    }

    private void initPowerSaveModeListener() {
        try {
            PowerSaveModeListener powerSaveModeListener = new PowerSaveModeListener(POWERSAVE_MODE_PATH);
            this.mPowerSaveModeListener = powerSaveModeListener;
            powerSaveModeListener.startWatching();
        } catch (Exception e) {
            Slog.e(TAG, "initPowerSaveModeListener init failed");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isPowerSaveMode() {
        return this.mIsPowerSaveMode;
    }

    /* loaded from: classes.dex */
    private class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    FrameworkPowerSaveModeObserver.this.mIsPowerSaveMode = msg.arg1 == 1;
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PowerSaveModeListener extends FileObserver {
        public PowerSaveModeListener(String path) {
            super(path);
            FrameworkPowerSaveModeObserver.this.checkPowerSaveModeState(path);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            switch (event) {
                case 2:
                    FrameworkPowerSaveModeObserver.this.checkPowerSaveModeState(FrameworkPowerSaveModeObserver.POWERSAVE_MODE_PATH);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPowerSaveModeState(String path) {
        try {
            String line = getContentFromFile(path);
            if (line != null) {
                int power_save_mode = Integer.parseInt(line);
                Slog.d(TAG, "power_save_mode is: " + power_save_mode + " path: " + path);
                if (POWERSAVE_MODE_PATH.equals(path)) {
                    this.mHandler.removeMessages(1001);
                    Message message = this.mHandler.obtainMessage(1001, power_save_mode, 0);
                    this.mHandler.sendMessage(message);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "checkPowerSaveModeState: ", e);
        }
    }

    private String getContentFromFile(String filePath) {
        StringBuilder sb;
        FileInputStream is = null;
        String content = null;
        try {
            try {
                try {
                    File file = new File(filePath);
                    is = new FileInputStream(file);
                    byte[] data = readInputStream(is);
                    content = new String(data).trim();
                    Slog.d(TAG, filePath + " content is " + content);
                    try {
                        is.close();
                    } catch (IOException e) {
                        e = e;
                        sb = new StringBuilder();
                        Slog.w(TAG, sb.append("can not close FileInputStream ").append(e).toString());
                        return content;
                    }
                } catch (Throwable th) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e2) {
                            Slog.w(TAG, "can not close FileInputStream " + e2);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e3) {
                Slog.w(TAG, "can't find file " + filePath + e3);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e4) {
                        e = e4;
                        sb = new StringBuilder();
                        Slog.w(TAG, sb.append("can not close FileInputStream ").append(e).toString());
                        return content;
                    }
                }
            }
        } catch (IndexOutOfBoundsException e5) {
            Slog.w(TAG, "index exception: " + e5);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    e = e6;
                    sb = new StringBuilder();
                    Slog.w(TAG, sb.append("can not close FileInputStream ").append(e).toString());
                    return content;
                }
            }
        }
        return content;
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
                    Slog.w(TAG, "readInputStream " + e);
                    try {
                        byteStream.close();
                        return null;
                    } catch (IOException e2) {
                        Slog.w(TAG, "can not close readInputStream " + e2);
                        return null;
                    }
                }
            } catch (Throwable th) {
                try {
                    byteStream.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "can not close readInputStream " + e3);
                }
                throw th;
            }
        }
        byte[] byteArray = byteStream.toByteArray();
        try {
            byteStream.close();
        } catch (IOException e4) {
            Slog.w(TAG, "can not close readInputStream " + e4);
        }
        return byteArray;
    }
}
