package com.android.server.input;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import com.android.server.LocalServices;
import com.android.server.policy.PhoneWindowManagerStub;

/* loaded from: classes.dex */
public class MiInputPhotoHandleManager {
    private static final String ACTION_HANDLE_STATE_CHANGED = "miui.intent.action.ACTION_HANDLE_STATE_CHANGED";
    private static final String EXTRA_HANDLE_CONNECT_STATE = "miui.intent.extra.EXTRA_HANDLE_CONNECT_STATE";
    private static final String EXTRA_HANDLE_PID = "pid";
    private static final String EXTRA_HANDLE_VID = "vid";
    private static final int HANDLE_STATUS_CONNECTION = 1;
    private static final int HANDLE_STATUS_DISCONNECTION = 0;
    public static final String PHOTO_HANDLE_HAS_BEEN_CONNECTED = "photo_handle_has_been_connected";
    private static final String TAG = "MiInputPhotoHandleManager";
    private static MiInputPhotoHandleManager sMiInputPhotoHandleManager;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private Handler mHandler;
    private MiuiInputManagerInternal mMiuiInputManagerInternal;
    private int mPhotoHandleConnectionStatus;
    private boolean mPhotoHandleHasConnected;
    private final BroadcastReceiver mPhotoHandleStatusReceiver = new BroadcastReceiver() { // from class: com.android.server.input.MiInputPhotoHandleManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int connectionStatus = intent.getIntExtra(MiInputPhotoHandleManager.EXTRA_HANDLE_CONNECT_STATE, 0);
            Slog.d(MiInputPhotoHandleManager.TAG, "receive handle status,connection=" + connectionStatus);
            if (MiInputPhotoHandleManager.this.mPhotoHandleConnectionStatus != connectionStatus) {
                MiInputPhotoHandleManager.this.mPhotoHandleConnectionStatus = connectionStatus;
                int pid = intent.getIntExtra(MiInputPhotoHandleManager.EXTRA_HANDLE_PID, 0);
                int vid = intent.getIntExtra(MiInputPhotoHandleManager.EXTRA_HANDLE_VID, 0);
                Message message = Message.obtain(MiInputPhotoHandleManager.this.mHandler, connectionStatus, pid, vid);
                MiInputPhotoHandleManager.this.mHandler.sendMessage(message);
            }
        }
    };

    public static MiInputPhotoHandleManager getInstance(Context context) {
        MiInputPhotoHandleManager miInputPhotoHandleManager;
        synchronized (MiInputPhotoHandleManager.class) {
            if (sMiInputPhotoHandleManager == null) {
                sMiInputPhotoHandleManager = new MiInputPhotoHandleManager(context);
            }
            miInputPhotoHandleManager = sMiInputPhotoHandleManager;
        }
        return miInputPhotoHandleManager;
    }

    private MiInputPhotoHandleManager(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public void init() {
        Slog.d(TAG, "init photo handle manager");
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
        this.mHandler = new HandleStatusSynchronize(MiuiInputThread.getHandler().getLooper());
        initIntentFilter();
        this.mPhotoHandleHasConnected = Settings.System.getInt(this.mContentResolver, PHOTO_HANDLE_HAS_BEEN_CONNECTED, 0) == 1;
    }

    public int getPhotoHandleDeviceId(int pid, int vid) {
        InputManager inputManager = (InputManager) this.mContext.getSystemService(InputManager.class);
        if (inputManager == null) {
            Slog.d(TAG, "inputManager is null");
        } else {
            for (int deviceId : inputManager.getInputDeviceIds()) {
                InputDevice inputDevice = inputManager.getInputDevice(deviceId);
                if (inputDevice.getProductId() == pid && inputDevice.getVendorId() == vid) {
                    return deviceId;
                }
            }
        }
        Slog.d(TAG, "not found deviceId");
        return -1;
    }

    private void initIntentFilter() {
        IntentFilter photoHandleFilter = new IntentFilter();
        photoHandleFilter.addAction(ACTION_HANDLE_STATE_CHANGED);
        this.mContext.registerReceiver(this.mPhotoHandleStatusReceiver, photoHandleFilter, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyHandleConnectStatus(boolean connection, int pid, int vid) {
        int deviceId = getPhotoHandleDeviceId(pid, vid);
        this.mMiuiInputManagerInternal.notifyPhotoHandleConnectionStatus(connection, deviceId);
        PhoneWindowManagerStub.getInstance().notifyPhotoHandleConnectionStatus(connection, deviceId);
        if (!this.mPhotoHandleHasConnected) {
            Settings.System.putInt(this.mContentResolver, PHOTO_HANDLE_HAS_BEEN_CONNECTED, 1);
            this.mPhotoHandleHasConnected = true;
        }
        Slog.d(TAG, "notifyHandleConnectStatus, connection=" + connection + " pid=" + pid + " vid=" + vid + " deviceId=" + deviceId);
    }

    public boolean isPhotoHandleHasConnected() {
        return this.mPhotoHandleHasConnected;
    }

    /* loaded from: classes.dex */
    private class HandleStatusSynchronize extends Handler {
        public HandleStatusSynchronize(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    MiInputPhotoHandleManager.this.notifyHandleConnectStatus(false, msg.arg1, msg.arg2);
                    return;
                case 1:
                    MiInputPhotoHandleManager.this.notifyHandleConnectStatus(true, msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }
}
